package com.planty.service.diary;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.planty.common.prompt.OpenAiService;
import com.planty.common.prompt.PromptKey;
import com.planty.dto.diary.DiaryAnalysisDto;
import com.planty.dto.diary.DiaryAnalysisResDto;
import com.planty.entity.crop.Crop;
import com.planty.repository.crop.CropRepository;
import com.planty.repository.diary.DiaryRepository;
import com.planty.repository.user.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

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
}
