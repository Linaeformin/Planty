package com.planty.dto.diary;

import com.planty.common.prompt.PromptKey;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;


// DiaryAnalysisFormDto와 이미지를 합치는 DTO
@Getter @Setter
public class DiaryAnalysisDto {
    private PromptKey promptKey;
    private Integer cropId;
    private String cropName;
    private MultipartFile image;
}
