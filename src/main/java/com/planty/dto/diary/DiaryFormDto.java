package com.planty.dto.diary;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;


// 프론트가 보내는 재배 일지 데이터
@Getter @Setter
public class DiaryFormDto {
    @NotBlank(message = "제목을 입력해주세요.")
    private String title;

    @NotBlank(message = "내용을 입력해주세요.")
    private String content;

    @Nullable
    private String analysis;
}
