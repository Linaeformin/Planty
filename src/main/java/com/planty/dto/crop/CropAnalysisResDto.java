package com.planty.dto.crop;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;


// 작물 이미지 분석 시 프론트에게 전달하는 DTO
@Getter @Setter
public class CropAnalysisResDto {
    private String cropName;
    private String environment;
    private String temperature;
    private String tall;
    private String howTo;
    private LocalDate startAt;
    private LocalDate endAt;
}
