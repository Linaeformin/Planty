package com.planty.dto.diary;

import com.planty.entity.diary.Diary;
import com.planty.entity.diary.DiaryImage;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;


// 작물 정보 페이지에서 재배 일지 리스트
@Getter @Setter
@Builder
public class DiaryListDto {
    private Integer diaryId;
    private String title;
    private String content;
    private String imageUrl;

    // 엔티티 -> DTO 반환
    public static DiaryListDto of(Diary diary) {

        // 미리보기 한 줄 추출
        String content = (diary.getContent()).split("\\.")[0]+".";

        // 썸네일 이미지 찾기
        String thumbnailUrl = diary.getImages().stream()
                .filter(DiaryImage::getThumbnail) // thumbnail == true
                .findFirst()
                .map(DiaryImage::getDiaryImg)     // boardImg 값 추출
                .orElse(null);

        // 게시글 미리보기 데이터 반환
        return DiaryListDto.builder()
                .diaryId(diary.getId())
                .title(diary.getTitle())
                .content(content)
                .imageUrl(thumbnailUrl)
                .build();
    }
}
