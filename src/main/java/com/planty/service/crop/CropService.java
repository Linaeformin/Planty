package com.planty.service.crop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.planty.common.prompt.OpenAiService;
import com.planty.common.prompt.PromptKey;
import com.planty.dto.crop.*;
import com.planty.entity.board.Board;
import com.planty.entity.crop.Crop;
import com.planty.entity.user.User;
import com.planty.repository.crop.CropRepository;
import com.planty.repository.user.UserRepository;
import com.planty.storage.StorageService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;


// 작물 서비스
@Service
@Transactional
@RequiredArgsConstructor
public class CropService {

    private final CropRepository cropRepository;
    private final StorageService storageService;
    private final OpenAiService openAiService;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;

    // 이미지와 작물 이름을 받아서 OpenAI API로 분석 요청 후 결과를 DTO로 변환하는 메소드
    public CropAnalysisResDto analyze(CropAnalysisDto formDto) {
        // 1) 이미지 파일 검증
        if (formDto.getImageFile() == null || formDto.getImageFile().isEmpty()) {
            throw new IllegalArgumentException("이미지를 삽입해주세요.");
        }

        // 2) cropName 변수를 Map 형태로 준비
        var vars = java.util.Map.of(
                "cropName", formDto.getCropName() == null ? "" : formDto.getCropName()
        );

        // 3) OpenAI API 호출 (프롬프트 키, 변수, 이미지 파일 전달)
        String raw = openAiService.callOpenAi(
                PromptKey.PLANT_ANALYSIS,
                vars,
                formDto.getImageFile()
        );

        // 4) 모델 응답에서 텍스트 추출
        String modelTextJson;
        try {
            // OpenAI 응답 파싱
            JsonNode root = objectMapper.readTree(raw);
            // 응답 구조 중 "/output/0/content/0/text" 경로에서 텍스트 가져오기
            modelTextJson = root.at("/output/0/content/0/text").asText();
            if (modelTextJson == null || modelTextJson.isBlank()) {
                // 해당 경로에 텍스트가 없으면 전체 JSON을 문자열로 사용
                modelTextJson = root.toString();
            }
        } catch (Exception e) {
            // JSON 파싱이 실패하면 원본 문자열 그대로 사용
            modelTextJson = raw;
        }

        // 5) 결과 DTO 생성 및 기본 값 세팅
        CropAnalysisResDto res = new CropAnalysisResDto();
        res.setCropName(formDto.getCropName());
        res.setStartAt(formDto.getStartAt());
        res.setEndAt(formDto.getEndAt());

        // 6) 모델이 반환한 JSON 텍스트를 파싱해서 필드에 매핑
        try {
            JsonNode j = objectMapper.readTree(modelTextJson);

            // 1) 1차 매핑
            res.setEnvironment(j.path("environment").asText(""));    // 환경
            res.setTemperature(asStringFlexible(j.get("temperature")));    // 온도
            res.setTall(asStringFlexible(j.get("tall")));    // 키
            res.setHowTo(j.path("howTo").asText(""));    // 재배 방법

            // 2) 보정: howTo 안에 JSON이 들어있다면 꺼내서 빈 칸 채우기
            if (needsMerge(res)) {
                mergeFromHowToJsonIfPresent(res.getHowTo(), res);
            }

        } catch (Exception e) {
            // JSON 파싱 실패 시, 빈 값으로 처리
            res.setEnvironment("");
            res.setTemperature("");
            res.setTall("");
            res.setHowTo(modelTextJson);    // 실패했을 경우 원본 문자열 전체를 howTo에 넣음
        }

        // 분석 결과 반환
        return res;
    }

    // 작물 저장
    public void saveCrop(Integer userId, CropRegisterDto res) throws IOException {
        // 작물을 등록하려는 유저 검증
        User user = userRepository.getReferenceById(userId);

        // 파일 URL 초기화
        String fileUrl = null;
        if (res.getImageFile() != null && !res.getImageFile().isEmpty()) {
            // "crops"라는 폴더에 저장 (uploads/crops/...)
            fileUrl = storageService.save(res.getImageFile(), "crops");
        }

        // 작물 객체 생성 및 데이터 삽입
        Crop crop = new Crop();
        crop.setUser(user);
        crop.setName(res.getCropName());
        crop.setCropImg(fileUrl); // DB에는 URL 문자열만 저장
        crop.setEnvironment(res.getEnvironment());
        crop.setTemperature(res.getTemperature());
        crop.setHeight(res.getHeight());
        crop.setHowTo(res.getHowTo());
        crop.setHarvest(false);
        crop.setStartAt(res.getStartAt());
        crop.setEndAt(res.getEndAt());

        // 작물 저장
        cropRepository.save(crop);
    }

    // 재배 상태 업데이트
    public void updateHarvest(Integer cropId,
                              Integer meId,
                              Boolean harvest) {

        // 대상 작물 조회 및 소유자 검증
        Crop crop = requireOwnCrop(cropId, meId);

        // 재배 상태 변경
        crop.setHarvest(harvest);

        // 저장
        cropRepository.save(crop);
    }

    // 작물 수정
    public void updateCrop(Integer cropId, Integer meId, CropUpdateFormDto dto, MultipartFile imageFile) throws IOException {
        // 작물 조회 및 소유권 검증
        Crop crop = requireOwnCrop(cropId, meId);

        // 1) 날짜 갱신 (보낸 값만)
        if (dto.getStartAt() != null) {
            crop.setStartAt(dto.getStartAt());
        }
        if (dto.getEndAt() != null) {
            crop.setEndAt(dto.getEndAt());
        }

        // 2) 이미지 갱신 규칙 (보낸 값만)
        if (imageFile != null && !imageFile.isEmpty()) {
            // 새 파일 업로드
            String oldUrl = crop.getCropImg();
            String newUrl = storageService.save(imageFile, "crops");
            crop.setCropImg(newUrl);

            // 기존 파일 정리
            if (oldUrl != null && !oldUrl.isBlank()) {
                try { storageService.deleteByUrl(oldUrl); } catch (Exception ignore) {}
            }
        }

        // 작물 수정 저장
        cropRepository.save(crop);
    }

    // 작물 삭제
    public void deleteCrop(Integer cropId, Integer meId) {
        // 대상 작물 조회 및 소유권 검증
        Crop crop = requireOwnCrop(cropId, meId);

        // 해당 작물 삭제
        cropRepository.delete(crop);
    }

    // 작물 조회 및 소유자 검증
    private Crop requireOwnCrop(Integer cropId,
                                  Integer meId){
        // 1) 대상 작물 조회
        Crop crop = cropRepository.findById(cropId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "NOT_FOUND"));

        // 2) 소유자 검증
        if (!crop.getUser().getId().equals(meId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "FORBIDDEN");
        }

        return crop;
    }

    // 모델이 반환한 JSON을 모두 문자열로 반환
    private String asStringFlexible(JsonNode node) {
        if (node == null || node.isNull()) return "";
        if (node.isTextual()) return node.asText();
        if (node.isNumber()) return node.numberValue().toString();
        return node.toString();
    }

    // resDto 안에 비어있는 값이 있으면 merge가 필요하다고 반환
    private boolean needsMerge(CropAnalysisResDto r) {
        return isBlank(r.getEnvironment()) || isBlank(r.getTemperature()) || isBlank(r.getTall());
    }

    // null이나 공백만 있는 경우도 빈 문자열로 취급
    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    // json 마크다운 코드 블럭을 벗기고 중괄호 구간만 추출해 JSON 파싱 시도
    private void mergeFromHowToJsonIfPresent(String howToRaw, CropAnalysisResDto res) {
        if (isBlank(howToRaw)) return;

        String cleaned = howToRaw
                .replaceAll("(?is)```json", "")
                .replaceAll("(?is)```", "")
                .trim();

        // howTo 안에서 첫 '{' ~ 마지막 '}' 구간만 뽑기 (느슨한 보호막)
        int s = cleaned.indexOf('{');
        int e = cleaned.lastIndexOf('}');
        if (s < 0 || e < s) return; // JSON 객체 없음

        String maybeJson = cleaned.substring(s, e + 1);
        try {
            JsonNode x = objectMapper.readTree(maybeJson);

            // 비어있는 필드만 채우기 (이미 값 있으면 건드리지 않음)
            if (isBlank(res.getEnvironment())) {
                res.setEnvironment(x.path("environment").asText(res.getEnvironment()));
            }
            if (isBlank(res.getTemperature())) {
                res.setTemperature(asStringFlexible(x.get("temperature")));
            }
            if (isBlank(res.getTall())) {
                res.setTall(asStringFlexible(x.get("tall")));
            }

            // howTo 본문이 JSON에도 있으면 교체, 없으면 기존 유지
            String howToText = x.path("howTo").asText(null);
            if (!isBlank(howToText)) {
                res.setHowTo(howToText);
            } else {
                // 코드블럭 표식만 벗겨서 깔끔히
                res.setHowTo(cleaned);
            }
        } catch (Exception ignore) {
            // JSON 파싱 실패면 그대로 둠 (howTo는 원문 유지)
        }
    }

    // 홈의 내가 등록한 작물 조회
    public List<CropHomeResDto> getHomeCrops(Integer userId) throws IOException {
        // 내가 등록한 작물 중 재배 되지 않은 작물
        List<Crop> crops = cropRepository.findByUser_IdAndHarvestFalseOrderByCreatedAtDesc(userId);

        // DTO로 반환
        return crops.stream()
                .map(CropHomeResDto::of)
                .toList();
    }


    // TODO: S3로 변환 시 이미지 URL 변환 로직으로 교체 (실행 확인 X)
//    // 1) 임시 업로드 → URL 확보
//    String imageUrl = storageService.store(formDto.getImageFile()); // 프리사인드/퍼블릭 URL
//
//    // 2) 프롬프트 변수(선택)
//    var vars = java.util.Map.of(
//            "cropName", formDto.getCropName() == null ? "" : formDto.getCropName(),
//            "imageUrl", imageUrl
//    );
//
//    // 3) URL로 호출 (파일은 더이상 넘기지 않음)
//    String raw = openAiService.callOpenAiWithImageUrl(
//            PromptKey.PLANT_ANALYSIS, vars, imageUrl
//    );
}
