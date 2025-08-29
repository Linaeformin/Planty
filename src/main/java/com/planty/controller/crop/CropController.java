package com.planty.controller.crop;

import com.planty.dto.crop.CropAnalysisDto;
import com.planty.dto.crop.CropAnalysisFormDto;
import com.planty.dto.crop.CropAnalysisResDto;
import com.planty.service.crop.CropService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;


// 작물
@RestController
@RequestMapping("/api/crop")
@RequiredArgsConstructor
public class CropController {

    private final CropService cropService;

    // 작물 이미지 분석
    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CropAnalysisResDto> analyze(
            @RequestPart("form") CropAnalysisFormDto form,    // JSON 파트
            @RequestPart("image") MultipartFile image    // 파일 파트
    ) {
        // 서비스가 기대하는 기존 FormDto로 합쳐서 전달
        CropAnalysisDto dto = new CropAnalysisDto();
        dto.setCropName(form.getCropName());
        dto.setStartAt(form.getStartAt());
        dto.setEndAt(form.getEndAt());
        dto.setImageFile(image);

        // 반환
        return ResponseEntity.ok(cropService.analyze(dto));
    }
}
