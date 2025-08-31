package com.planty.dto.crop;

import com.planty.dto.diary.DiaryListDto;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.util.List;


// 작물 정보 전체 데이터 프론트 전달용
@Getter @Setter
@Builder
public class CropDetailsResDto {
    private CropDetailsDto crop;
    private List<DiaryListDto> diaries;
}
