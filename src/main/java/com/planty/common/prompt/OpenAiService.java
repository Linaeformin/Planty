package com.planty.common.prompt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.Nullable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

import com.planty.config.AiProperties;
import lombok.RequiredArgsConstructor;


// AI 사용 서비스
@Service
@RequiredArgsConstructor
public class OpenAiService {

    private final PromptRegistry prompts;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper mapper;
    private final AiProperties aiProps;

    public String callOpenAi(PromptKey key, Map<String, String> variables, @Nullable MultipartFile file) {

        // 프리셋이 없을 때
        if (!key.hasPreset()) {
            throw new IllegalArgumentException("프리셋 없는 PromptKey: " + key);
        }

        // 프리셋으로 OpenAi 호출
        var preset = key.getPreset(aiProps);
        return callOpenAi(preset.getModel(), preset.getMaxTokens(), preset.getTemperature(), key, variables, file);
    }

    // OpenAI 호출
    public String callOpenAi(String model, int maxTokens, double temperature,
                             PromptKey key, Map<String, String> variables, @Nullable MultipartFile file) {

        // 프롬프트 세팅
        PromptSet set = prompts.get(key);

        // 등록된 프롬프트가 아닌 경우 예외 처리
        if (set == null) throw new IllegalArgumentException("등록되지 않은 PromptKey: " + key);

        // 1) user 템플릿 변수 치환
        String user = Optional.ofNullable(set.getUser()).orElse("");

        // Map에 들어 있는 모든 key와 value 쌍을 꺼내여 키에 맞는 value 삽입
        if (variables != null) {
            for (var e : variables.entrySet()) {
                user = user.replace("{{" + e.getKey() + "}}", e.getValue());
            }
        }

        // 2) parts 조립
        ArrayNode parts = mapper.createArrayNode();

        // 텍스트 파트
        if (!user.isBlank()) {
            ObjectNode textPart = mapper.createObjectNode();
            textPart.put("type", "input_text");
            textPart.put("text", user);
            parts.add(textPart);
        }

        // 이미지 파트
        if (file != null && !file.isEmpty()) {
            String imageUrl = toDataUri(file); // ← 여기서 분리
            ObjectNode imagePart = mapper.createObjectNode();
            imagePart.put("type", "input_image");
            imagePart.put("image_url", imageUrl);
            parts.add(imagePart);
        }

        // 최종 input
        ArrayNode input = mapper.createArrayNode();
        input.add(msg("system", set.getSystem()));
        input.add(msg("user", parts));

        ObjectNode body = mapper.createObjectNode();
        body.put("model", model);
        body.set("input", input);
        body.put("max_output_tokens", maxTokens);
        body.put("temperature", temperature);

        // 3) WebClient로 API 호출
        String raw = webClientBuilder
                .baseUrl(aiProps.getOpenai().getUrl())
                .defaultHeader("Authorization", "Bearer " + aiProps.getOpenai().getApiKey())
                .build()
                .post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)

                // 응답 추출 모드 (상태 코드에 따라 에러로 변환해서 시그널 내보내기)
                .retrieve()

                // 응답 바디를 문자열로 디코딩해서 Mono<String>에 답기
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(aiProps.getOpenai().getTimeout()))
                .retry(aiProps.getOpenai().getMaxRetries())
                .block();

        // 응답 없을 때 예외 처리
        if (raw == null) throw new RuntimeException("OpenAI 응답이 비어있음");


        // 4) 결과 추출 (Responses API 포맷)
        try {
            JsonNode root = mapper.readTree(raw);
            return root.toString();
        } catch (Exception e) {
            return raw;
        }
    }

    // system, user 텍스트 메시지 전용
    private ObjectNode msg(String role, String content) {
        ObjectNode n = mapper.createObjectNode();
        n.put("role", role);
        n.put("content", content);
        return n;
    }

    // 멀티모달 파트(ArrayNode) 메시지 전용
    private ObjectNode msg(String role, ArrayNode parts) {
        ObjectNode n = mapper.createObjectNode();
        n.put("role", role);
        n.set("content", parts);
        return n;
    }

    // 로컬 개발: MultipartFile → data URI(Base64) (이미지 분석용)
    public String toDataUri(MultipartFile file) {
        try {
            String mime = Optional.ofNullable(file.getContentType()).orElse("image/jpeg");
            String b64  = Base64.getEncoder().encodeToString(file.getBytes());
            return "data:" + mime + ";base64," + b64;
        } catch (IOException e) {
            throw new RuntimeException("이미지 인코딩 실패", e);
        }


    }

    // TODO: S3로 변환 시 이미지 URL 변환 로직으로 교체 (실행 확인 X)
//    public String callOpenAiWithImageUrl(PromptKey key, Map<String,String> variables, String imageUrl) {
//        var preset = key.getPreset(aiProps);
//        return callOpenAiWithImageUrl(preset.getModel(), preset.getMaxTokens(), preset.getTemperature(),
//                key, variables, imageUrl);
//    }
//
//    private String callOpenAiWithImageUrl(String model, int maxTokens, double temperature,
//                                          PromptKey key, Map<String,String> variables, String imageUrl) {
//        var set = prompts.get(key);
//        String user = Optional.ofNullable(set.getUser()).orElse("");
//        if (variables != null) for (var e : variables.entrySet()) user = user.replace("{{"+e.getKey()+"}}", e.getValue());
//
//        ArrayNode parts = mapper.createArrayNode();
//        if (!user.isBlank()) parts.add(object("input_text", "text", user));
//        if (imageUrl != null && !imageUrl.isBlank()) {
//            ObjectNode img = mapper.createObjectNode();
//            img.put("type", "input_image");
//            img.put("image_url", imageUrl);         // ← 파일 대신 URL
//            parts.add(img);
//        }
//
//        ArrayNode input = mapper.createArrayNode();
//        input.add(msg("system", set.getSystem()));
//        input.add(msg("user", parts));
//
//        ObjectNode body = mapper.createObjectNode();
//        body.put("model", model);
//        body.set("input", input);
//        body.put("max_output_tokens", maxTokens);
//        body.put("temperature", temperature);
//
//        String raw = webClientBuilder.baseUrl(aiProps.getOpenai().getUrl())
//                .defaultHeader("Authorization","Bearer "+aiProps.getOpenai().getApiKey())
//                .build().post().contentType(MediaType.APPLICATION_JSON).bodyValue(body)
//                .retrieve().bodyToMono(String.class)
//                .timeout(Duration.ofSeconds(aiProps.getOpenai().getTimeout()))
//                .retry(aiProps.getOpenai().getMaxRetries()).block();
//
//        if (raw == null) throw new RuntimeException("OpenAI 응답이 비어있음");
//        try { return mapper.readTree(raw).toString(); } catch (Exception e) { return raw; }
//    }
//
//    private ObjectNode object(String type, String k, String v){
//        ObjectNode n = mapper.createObjectNode();
//        n.put("type", type); n.put(k, v); return n;
//    }
}
