package com.planty.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


// 각 프리셋 매핑
@Getter @Setter
@Configuration
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    // 각 상황별 프리셋 생성
    private OpenAi openai = new OpenAi();
    private Preset plantAnalysis = new Preset();
    private Preset cropDiagnosis = new Preset();
    private Preset diaryIntegrated = new Preset();
    private Preset diaryDisease = new Preset();
    private Preset diaryMarket = new Preset();

    // API 호출 기본 정보
    @Getter @Setter
    public static class OpenAi {
        private String apiKey;
        private String url;
        private int timeout;
        private int maxRetries;
    }

    // AI 설정 세트
    @Getter @Setter
    public static class Preset {
        private String model;
        private int maxTokens;
        private double temperature;
    }
}
