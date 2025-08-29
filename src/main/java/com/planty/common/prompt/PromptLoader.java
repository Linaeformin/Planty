package com.planty.common.prompt;

import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;


// AI 프롬프트를 txt에서 String으로 가져오는 클래스
@Component
public class PromptLoader {

    // ResourceLoader: 스프링이 자동으로 주입해주는 파일 리소스 접근 도구
    private final ResourceLoader resourceLoader;

    public PromptLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    // 주어진 경로의 프롬프트 파일을 상대 경로로 읽어서 파일 전체 내용을 문자열로 반환
    public String load(String path) {
        // classpath:/prompt/...txt 경로의 파일을 InputStream으로 열기
        try (InputStream in = resourceLoader.getResource("classpath:" + path).getInputStream()) {
            // InputStream → byte[] → UTF-8 문자열로 변환
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);

        } catch (IOException e) {
            // 파일이 없거나 읽기 실패 시 런타임 예외로 변환
            throw new RuntimeException("프롬프트 파일을 읽을 수 없습니다: " + path, e);
        }
    }
}
