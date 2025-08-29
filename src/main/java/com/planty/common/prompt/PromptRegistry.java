package com.planty.common.prompt;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;


// 프롬프트(system + user)를 미리 로드, PromptKey로 쉽게 꺼내 쓸 수 있게 관리
@Component
public class PromptRegistry {

    private final PromptLoader loader;
    private final Map<PromptKey, PromptSet> cache = new EnumMap<>(PromptKey.class);

    // 생성자에서 PromptLoader 주입
    public PromptRegistry(PromptLoader loader) {
        this.loader = loader;
        init(); // 앱 시작 시 프롬프트 파일들을 전부 읽어와서 cache에 저장
    }

    // 앱 시작 시 프롬프트 파일들을 전부 읽어와서 cache에 저장
    private void init() {
        // 작물 분석용 system/user 프롬프트 세트 등록
        cache.put(
                PromptKey.PLANT_ANALYSIS,
                new PromptSet(
                        loader.load("prompt/plant-analysis.system.txt"),
                        loader.load("prompt/plant-analysis.user.txt")
                )
        );

        // 일지 통합 분석용 system/user 프롬프트 세트 등록
        cache.put(
                PromptKey.DIARY_INTEGRATED,
                new PromptSet(
                        loader.load("prompt/diary-integrated.system.txt"),
                        loader.load("prompt/diary-integrated.user.txt")
                )
        );

        // 일지 질병 분석용 system/user 프롬프트 세트 등록
        cache.put(
                PromptKey.DIARY_DISEASE,
                new PromptSet(
                        loader.load("prompt/diary-disease.system.txt"),
                        loader.load("prompt/diary-disease.user.txt")
                )
        );

        // 일지 시장성 분석용 system/user 프롬프트 세트 등록
        cache.put(
                PromptKey.DIARY_MARKET,
                new PromptSet(
                        loader.load("prompt/diary-market.system.txt"),
                        loader.load("prompt/diary-market.user.txt")
                )
        );

        // 채팅은 user 템플릿이 필요 없으니까 null 허용
        cache.put(
                PromptKey.AI_CHAT,
                new PromptSet(
                        loader.load("prompt/ai-chat.system.txt"),
                        null
                )
        );
    }

    // 등록된 프롬프트 세트 꺼내오기
    public PromptSet get(PromptKey key) {
        return cache.get(key);
    }
}
