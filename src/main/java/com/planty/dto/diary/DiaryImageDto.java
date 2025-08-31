package com.planty.dto.diary;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;


// 재배 일지 이미지 DTO
@Getter @Setter
@Builder
public class DiaryImageDto {
    private Integer id;
    private String url;
}
