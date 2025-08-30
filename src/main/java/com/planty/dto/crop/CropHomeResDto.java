package com.planty.dto.crop;

import com.planty.entity.crop.Crop;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;


// 프론트에게 전달해주는 홈의 작물 정보
@Getter @Setter
@Builder
public class CropHomeResDto {
    private Integer cropId;
    private String cropName;
    private String cropImage;
    private String period;

    // 엔티티 -> DTO 반환
    public static CropHomeResDto of(Crop crop) {
        // crop 엔티티에서 LocalDate 꺼내오기
        LocalDate date = crop.getEndAt();
        int year = date.getYear();
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        // 초, 중, 하순 판별
        String periodStr;
        if (day <= 10) {
            periodStr = "초순";
        } else if (day <= 20) {
            periodStr = "중순";
        } else {
            periodStr = "하순";
        }

        // 날짜 데이터 결합
        String formattedPeriod = String.format("%d년 %d월 %s", year, month, periodStr);

        // 반환
        return CropHomeResDto.builder()
                .cropId(crop.getId())
                .cropName(crop.getName())
                .cropImage(crop.getCropImg())
                .period(formattedPeriod)
                .build();
    }

}
