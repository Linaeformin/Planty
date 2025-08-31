package com.planty.dto.diary;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;


// 재배 일지 저장용 중간 데이터
@Getter @Setter
public class DiaryDto {
    private String title;
    private String content;
    private String analysis;
    private List<String> imageUrls = new ArrayList<>();
}
