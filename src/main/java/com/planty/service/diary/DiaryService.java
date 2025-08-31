package com.planty.service.diary;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.planty.common.prompt.OpenAiService;
import com.planty.common.prompt.PromptKey;
import com.planty.dto.diary.*;
import com.planty.entity.board.BoardImage;
import com.planty.entity.crop.Crop;
import com.planty.entity.diary.Diary;
import com.planty.entity.diary.DiaryImage;
import com.planty.entity.user.User;
import com.planty.repository.crop.CropRepository;
import com.planty.repository.diary.DiaryRepository;
import com.planty.repository.user.UserRepository;
import com.planty.storage.StorageService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

// 재배 일지 서비스
@Service
@Transactional
@RequiredArgsConstructor
public class DiaryService {

    private final DiaryRepository diaryRepository; // (지금은 미사용) 일지 저장/조회 시 확장 가능
    private final UserRepository userRepository;   // (지금은 미사용) 사용자 관련 확장용
    private final OpenAiService openAiService;     // OpenAI 호출 어댑터
    private final ObjectMapper objectMapper;       // JSON 파싱
    private final CropRepository cropRepository;   // 소유자 검증 및 작물 조회
    private final StorageService storageService;

    // 재배 일지 이미지 분석
    public DiaryAnalysisResDto diaryAnalysis(Integer meId, DiaryAnalysisDto dto) {
        // ── 0) 필수값 검증 ────────────────────────────
        if (dto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "BAD_REQUEST");
        }
        // 프롬프트 키
        if (dto.getPromptKey() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PROMPT_KEY_REQUIRED");
        }
        // 재배 일지 전용 키인지 검증
        if (!isDiaryPrompt(dto.getPromptKey())) {
            // 이번 API는 재배 일지 전용 키만 허용
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_PROMPT_KEY");
        }
        // CropId 존재 여부 확인
        if (dto.getCropId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CROP_ID_REQUIRED");
        }

        // 1) 소유 작물 검증
        requireOwnCrop(dto.getCropId(), meId);

        // 2) 이미지 파일 검증
        MultipartFile image = dto.getImage();
        if (image == null || image.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "IMAGE_REQUIRED");
        }

        // 3) 프롬프트에 cropName 변수 삽입
        Map<String, String> vars = Map.of("cropName", dto.getCropName() == null ? "" : dto.getCropName());

        // 4) OpenAI 호출
        String raw = openAiService.callOpenAi(
                dto.getPromptKey(),
                vars,
                image  // 이미지를 그대로 전송
        );

        // 5) 모델 응답에서 텍스트 추출
        String modelText = extractModelText(raw);

        // ── 6) 최종 결과 텍스트 확정 ──────────────────────────────────────
        String analysis = coerceAnalysis(modelText);

        // ── 7) 응답 DTO 매핑 ──────────────────────────────────────────────
        DiaryAnalysisResDto res = new DiaryAnalysisResDto();
        res.setPromptKey(dto.getPromptKey());
        res.setCropId(dto.getCropId());
        res.setAnalysis(analysis);

        return res;
    }

    // 재배 일지 작성
    public void saveDiary(Integer userId, DiaryDto dto, Integer cropId) {
        // 유저 권한 확인
        requireOwnCrop(cropId, userId);

        // 작성자 객체
        User user = userRepository.getReferenceById(userId);

        // 작물 객체
        Crop crop = cropRepository.getReferenceById(cropId);

        // 재배 일지 생성 및 데이터 삽입
        Diary diary = new Diary();
        diary.setTitle(dto.getTitle());
        diary.setContent(dto.getContent());
        diary.setUser(user);
        diary.setCrop(crop);

        // AI 분석 없이 재배 일지를 등록할 경우
        if(dto.getAnalysis() != null) diary.setAnalysis(dto.getAnalysis());

        // 재배 일지 이미지 삽입
        List<DiaryImage> imgs = new ArrayList<>();
        for (int i = 0; i < dto.getImageUrls().size(); i++) {
            DiaryImage di = new DiaryImage();
            di.setDiary(diary);
            di.setDiaryImg(dto.getImageUrls().get(i));
            di.setThumbnail(i == 0);
            imgs.add(di);
        }
        diary.setImages(imgs);

        // 재배 일지 저장
        diaryRepository.save(diary);
    }

    // 재배 일지 상세 조회
    public DiaryDetailsResDto getDiaryDetail(Integer userId, Integer cropId, Integer diaryId) {

        // 1) 다이어리 없으면 404
        Diary diary = diaryRepository.findDetailById(diaryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "NOT_FOUND"));

        // 2) 다이어리 이미지를 리스트로
        List<DiaryImageDto> imageDtos = diary.getImages() == null ? List.of()
                : diary.getImages().stream()
                .map(img -> DiaryImageDto.builder()
                        .id(img.getId())
                        .url(img.getDiaryImg())
                        .build())
                .toList();

        // 3) 소유자 판단
        Boolean isOwner = diary.getUser() != null && diary.getUser().getId().equals(userId);

        // 3) 프론트 응답용 DTO에 데이터 넣기
        DiaryDetailsResDto dto = DiaryDetailsResDto.builder()
                .diaryId(diary.getId())
                .title(diary.getTitle())
                .content(diary.getContent())
                .analysis(diary.getAnalysis())  // null이면 아예 빠짐 (@JsonInclude)
                .images(imageDtos)
                .isOwner(isOwner)
                .build();

        // 반환
        return dto;
    }

    // 재배 일지 수정
    public void updateDiary(Integer userId, DiaryDto dto, Integer diaryId, List<String> keepImageUrls, Integer cropId) {
        // 1) 대상 재배 일지 조회
        Diary diary = diaryRepository.findDetailById(diaryId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 일지입니다."));

        // 2) 재배 일지의 crop과 접근 경로의 crop 비교
        if (diary.getCrop() != null && !diary.getCrop().getId().equals(cropId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MISMATCH_CROP_ID");
        }

        // 3) 권한 체크 (내 재배 일지가 아닐 때)
        if (!diary.getUser().getId().equals(userId)) {
            throw new SecurityException("접근 권한이 없습니다.");
        }

        // 4) 본문 필드 업데이트 (null 허용 정책은 필요에 맞게 조정)
        if (dto.getTitle() != null)   diary.setTitle(dto.getTitle());
        if (dto.getContent() != null) diary.setContent(dto.getContent());
        if (dto.getAnalysis() != null)   diary.setAnalysis(dto.getAnalysis());

        // 5) 이미지 동기화
        // 현재 이미지
        List<DiaryImage> currentImages = diary.getImages() != null ? diary.getImages() : new ArrayList<>();

        // 유지할 목록 (null -> 빈 리스트)
        List<String> keep = keepImageUrls != null ? keepImageUrls : Collections.emptyList();

        // 새로 추가할 URL 목록 (컨트롤러에서 파일 업로드 후 전달된 것)
        List<String> newUrls = (dto.getImageUrls() != null) ? dto.getImageUrls() : Collections.emptyList();

        // (1) 삭제 대상 = 현재 - keep
        List<String> keepSet = new ArrayList<>(keep); // 순서 보존용

        List<DiaryImage> toRemove = new ArrayList<>();
        for (DiaryImage img : currentImages) {
            if (!keepSet.contains(img.getDiaryImg())) {
                toRemove.add(img);
            }
        }

        for (DiaryImage img : toRemove) {
            try {
                storageService.deleteByUrl(img.getDiaryImg());
            } catch (Exception ignored) {

            }
        }

        // DB 고아 삭제(orphanRemoval=true)와 함께
        currentImages.removeAll(toRemove);

        // (2) 새 이미지 추가 (중복 방지)
        // 이미 남아있는 URL 집합
        Set<String> remain = currentImages.stream().map(DiaryImage::getDiaryImg).collect(Collectors.toSet());
        Set<String> added = new HashSet<>();
        for (String url : newUrls) {
            if (url == null || url.isBlank()) continue;
            if (remain.contains(url) || added.contains(url)) continue;
            DiaryImage di = new DiaryImage();
            di.setDiary(diary);
            di.setDiaryImg(url);
            di.setThumbnail(false);
            currentImages.add(di);
            added.add(url);
        }

        // (3) 썸네일 재지정
        // 규칙: 최종 이미지 목록의 첫 번째 이미지를 thumbnail=true로
        setThumbnailByOrder(currentImages, keepSet, newUrls);

        // 6) 저장
        diaryRepository.save(diary);
    }

    // ─────────────────────────── 공통 유틸 ───────────────────────────

    // 재배 일지 전용 프롬프트 키만 허용
    private boolean isDiaryPrompt(PromptKey key) {
        return key == PromptKey.DIARY_INTEGRATED
                || key == PromptKey.DIARY_DISEASE
                || key == PromptKey.DIARY_MARKET;
    }

    // 우선 경로("/output/0/content/0/text") 시도 → 없으면 전체 JSON 문자열 → JSON 아님이면 원문 그대로
    private String extractModelText(String raw) {
        // 응답 데이터가 없을 떄
        if (raw == null) return "";

        try {
            // 응답 데이터를 JSON으로 파싱해서 트리 구조로 변환
            JsonNode root = objectMapper.readTree(raw);

            // JSON으로 변환한 데이터를 String으로 변환
            String text = root.at("/output/0/content/0/text").asText(null);

            // 경로가 없다면 raw 그대로
            return (text != null) ? text : raw; // ← 경로 없으면 raw 그대로

        } catch (Exception e) {
            return raw;
        }
    }

    // 모델 텍스트에서 analysis만 뽑아오기
    private String coerceAnalysis(String modelText) {
        if (modelText == null) return "";
        try {
            JsonNode j = objectMapper.readTree(modelText);

            // 분석 결과만 뽑아내기
            if (j.hasNonNull("analysis")) {
                String a = j.path("analysis").asText("");
                if (!a.isBlank()) return a;
            }

            // 키가 없거나 빈 문자열이면 JSON 원문(문제 상황 명확)
            return j.toString();

        } catch (Exception e) {
            // 모델이 JSON 형식을 안 지키면 원문 그대로 반환
            return modelText;
        }
    }

    // 작물 소유자 검증
    private void requireOwnCrop(Integer cropId, Integer meId) {
        // 존재하지 않는 작물일 때
        Crop crop = cropRepository.findById(cropId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "NOT_FOUND"));

        // 소유한 작물이 아닐 때
        if (!crop.getUser().getId().equals(meId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "FORBIDDEN");
        }
    }

    // 이미지 썸네일 지정
    private void setThumbnailByOrder(List<DiaryImage> images,
                                     List<String> keepImageUrls,
                                     List<String> newUrls) {
        // 전체 타겟 순서
        List<String> ordered = new ArrayList<>();
        if (keepImageUrls != null) ordered.addAll(keepImageUrls);
        if (newUrls != null) ordered.addAll(newUrls);

        // 먼저 모두 false
        for (DiaryImage img : images) {
            img.setThumbnail(false);
        }

        // ordered 기준으로 첫 번째로 매칭되는 이미지를 썸네일
        for (String first : ordered) {
            for (DiaryImage img : images) {
                if (first != null && first.equals(img.getDiaryImg())) {
                    img.setThumbnail(true);
                    return;
                }
            }
        }

        // ordered가 비었거나 매칭 실패 시, 남아있는 리스트 첫 번째를 썸네일
        if (!images.isEmpty()) {
            images.get(0).setThumbnail(true);
        }
    }
}
