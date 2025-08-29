package com.planty.dto.crop;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;


// 프론트에서 받아오는 이미지 등록 전 DTO
@Getter @Setter
public class CropAnalysisFormDto {
    private String cropName;
    private LocalDate startAt;
    private LocalDate endAt;
}

