package com.planty.dto.crop;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;


// 작물 수정 시 프론트가 보내는 Dto
@Getter @Setter
public class CropUpdateFormDto {
    private LocalDate startAt;
    private LocalDate endAt;
}
