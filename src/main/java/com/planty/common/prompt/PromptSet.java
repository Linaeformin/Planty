package com.planty.common.prompt;


// system + user 프롬프트 묶음
class PromptSet {
    // system 프롬프트
    private final String system;

    // user 프롬프트
    private final String user;

    // 생성자: system과 user 문자열을 받아서 객체를 만듦
    public PromptSet(String system, String user) {
        this.system = system;
        this.user = user;
    }

    // system 프롬프트 꺼내기
    public String getSystem() {
        return system;
    }

    // user 프롬프트 꺼내기
    public String getUser() {
        return user;
    }
}
