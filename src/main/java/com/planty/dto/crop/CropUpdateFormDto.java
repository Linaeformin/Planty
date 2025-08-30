package com.planty.dto.crop;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;


@Getter @Setter
public class CropUpdateFormDto {
    private LocalDate startAt;
    private LocalDate endAt;
}
