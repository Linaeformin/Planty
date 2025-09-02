package com.planty.dto.diary;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;


// 판매 게시글 -> 재배 일지 프론트에게 전달하는 DTO
@Getter @Setter
@Builder
public class DiaryDetailsResDto {
    private Integer diaryId;
    private String title;
    private String content;
    private List<String> images;
    private String time;
    private String analysis;
    private Boolean isOwner;
}

