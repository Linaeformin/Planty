package com.planty.dto.crop;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;


// 작물 등록용 프론트가 전달하는 DTO
@Getter @Setter
public class CropRegisterFormDto {
    private String cropName;
    private LocalDate startAt;
    private LocalDate endAt;
    private String howTo;
    private String environment;
    private String temperature;
    private String height;
}
