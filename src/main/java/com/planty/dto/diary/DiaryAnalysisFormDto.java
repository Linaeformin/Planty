package com.planty.dto.diary;

import com.planty.common.prompt.PromptKey;
import lombok.Getter;
import lombok.Setter;


// 프론트가 재배 일지 분석을 위해 보내주는 데이터
@Getter @Setter
public class DiaryAnalysisFormDto {
    private PromptKey promptKey;
    private String cropName;
    private Integer cropId;
}
