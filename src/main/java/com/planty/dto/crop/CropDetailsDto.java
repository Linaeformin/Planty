package com.planty.dto.crop;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;


// 작물 정보에서 불러오는 작물 디테일
@Getter @Setter
@Builder
public class CropDetailsDto {
    private Integer cropId;
    private String name;
    private String cropImg;
    private LocalDate startAt;
    private LocalDate endAt;
}
