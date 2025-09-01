package com.planty.dto.board;

import com.planty.entity.board.Board;
import com.planty.entity.board.BoardImage;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;


// 프론트 전달용 판매 게시글 목록 데이터
@Getter @Setter
@Builder
public class BoardAllResDto {
    private Integer boardId;
    private String title;
    private Integer price;
    private String thumbnailImg;
    private String time;
    private Boolean sell;

    // 엔티티 -> DTO 반환
    public static BoardAllResDto of(Board board) {
        // 현재 시간과 게시글이 등록된 시간으로 텍스트 반환
        Instant createdAt = board.getCreatedAt();
        String time = toTimeAgo(createdAt);

        // 썸네일 이미지 찾기
        String thumbnailUrl = board.getImages().stream()
                .filter(BoardImage::getThumbnail) // thumbnail == true
                .findFirst()
                .map(BoardImage::getBoardImg)     // boardImg 값 추출
                .orElse(null);

        // 게시글 미리보기 데이터 반환
        return BoardAllResDto.builder()
                .boardId(board.getId())
                .title(board.getTitle())
                .price(board.getPrice())
                .time(time)
                .thumbnailImg(thumbnailUrl)
                .sell(board.getSell())
                .build();
    }

    // 시간 계산
    public static String toTimeAgo(Instant instant) {
        // 모든 시간은 한국 시간 기준으로
        ZoneId zone = ZoneId.of("Asia/Seoul");

        // 게시글이 생성된 시각을 zone 기준으로 변환
        ZonedDateTime time = instant.atZone(zone);

        // 현재 시각을 zone 기준으로 변환
        ZonedDateTime now = ZonedDateTime.now(zone);

        // 게시글 작성 시간과 현재 시간의 차이를 구하기
        long years = ChronoUnit.YEARS.between(time, now);
        if (years > 0) return years + "년 전";

        long months = ChronoUnit.MONTHS.between(time, now);
        if (months > 0) return months + "달 전";

        long days = ChronoUnit.DAYS.between(time, now);
        if (days > 0) return days + "일 전";

        long hours = ChronoUnit.HOURS.between(time, now);
        if (hours > 0) return hours + "시간 전";

        long minutes = ChronoUnit.MINUTES.between(time, now);
        if (minutes > 0) return minutes + "분 전";

        return "방금 전";
    }
}
