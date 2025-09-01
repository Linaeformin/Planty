package com.planty.dto.board;

import com.planty.dto.crop.CropDetailsDto;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;


// 판매 게시글 수정 전 프론트에게 전달하는 DTO
@Getter @Setter
@Builder
public class BoardUpdateResDto {
    private CropDetailsDto cropDetailsDto;
    private BoardDetailDto boardDetailDto;
}
