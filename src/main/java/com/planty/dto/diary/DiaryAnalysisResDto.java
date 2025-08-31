package com.planty.dto.diary;

import com.planty.common.prompt.PromptKey;
import lombok.Getter;
import lombok.Setter;


// 다이어리 분석 결과를 프론트에게 전달하는 DTO
@Getter @Setter
public class DiaryAnalysisResDto {
    private PromptKey promptKey;
    private Integer cropId;
    private String analysis;
}
