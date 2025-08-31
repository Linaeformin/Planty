package com.planty.dto.diary;

import com.planty.entity.diary.DiaryImage;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.util.List;


// 재배 일지 상세 페이지 프론트에게 전달
@Getter @Setter
@Builder
public class DiaryDetailsResDto {
    private Integer diaryId;
    private String title;
    private String content;
    private String analysis;
    private List<DiaryImageDto> images;
}
