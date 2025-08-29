package com.planty.common.prompt;

import com.planty.config.AiProperties;


// 케이스에 따라 프롬프트를 구별하는 enum
public enum PromptKey {
    PLANT_ANALYSIS,
    DIARY_INTEGRATED,
    DIARY_DISEASE,
    DIARY_MARKET,
    AI_CHAT;

    // AI를 사용하는 곳에 따라 ai 설정 변경
    public AiProperties.Preset getPreset(AiProperties props) {
        return switch (this) {
            case PLANT_ANALYSIS   -> props.getPlantAnalysis();
            case DIARY_INTEGRATED -> props.getDiaryIntegrated();
            case DIARY_DISEASE    -> props.getDiaryDisease();
            case DIARY_MARKET     -> props.getDiaryMarket();
            case AI_CHAT          -> throw new IllegalArgumentException("프리셋이 없는 키: " + this);
        };
    }

    // AI 설정 프리셋이 필요한 경우에만 true 반환
    public boolean hasPreset() {
        return this != AI_CHAT;
    }
}

