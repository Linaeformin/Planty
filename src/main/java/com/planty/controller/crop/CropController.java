package com.planty.controller.crop;

import com.planty.common.ApiSuccess;
import com.planty.config.CustomUserDetails;
import com.planty.dto.crop.*;
import com.planty.service.crop.CropService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.codec.CodecProperties;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import retrofit2.http.Path;

import java.io.IOException;


// 작물
@RestController
@RequestMapping("/api/crop")
@RequiredArgsConstructor
public class CropController {

    private final CropService cropService;
    private final CodecProperties codecProperties;

    // 작물 이미지 분석
    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CropAnalysisResDto> analyze(
            @RequestPart("form") CropAnalysisFormDto form,    // JSON 파트
            @RequestPart("image") MultipartFile image,    // 파일 파트
            @AuthenticationPrincipal CustomUserDetails me
    ) {
        // 권한이 없을 때
        if (me == null) return ResponseEntity.status(401).build();

        // 서비스가 기대하는 기존 FormDto로 합쳐서 전달
        CropAnalysisDto dto = new CropAnalysisDto();
        dto.setCropName(form.getCropName());
        dto.setStartAt(form.getStartAt());
        dto.setEndAt(form.getEndAt());
        dto.setImageFile(image);

        // 반환
        return ResponseEntity.ok(cropService.analyze(dto));
    }

    // 작물 등록
    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> register(
            @AuthenticationPrincipal CustomUserDetails me,
            @RequestPart("form") CropRegisterFormDto form,    // JSON 파트
            @RequestPart("image") MultipartFile image
    ) throws IOException {
        // 권한이 없을 때
        if (me == null) return ResponseEntity.status(401).build();

        // 서비스가 기대하는 기존 FormDto로 합쳐서 전달
        CropRegisterDto dto = new CropRegisterDto();
        dto.setCropName(form.getCropName());
        dto.setStartAt(form.getStartAt());
        dto.setEndAt(form.getEndAt());
        dto.setHowTo(form.getHowTo());
        dto.setImageFile(image);
        dto.setEnvironment(form.getEnvironment());
        dto.setTemperature(form.getTemperature());
        dto.setHeight(form.getHeight());

        // DB에 저장
        cropService.saveCrop(me.getId(), dto);

        // 성공 응답
        return ResponseEntity.status(201).body(new ApiSuccess(201, "성공적으로 처리되었습니다."));
    }

    // 홈의 작물 목록
    @GetMapping(value = "")
    public ResponseEntity<?> getCrop(
            @AuthenticationPrincipal CustomUserDetails me
    ) throws IOException {
        // 권한이 없을 때
        if (me == null) return ResponseEntity.status(401).build();

        // 작물 정보 반환
        return ResponseEntity.ok(cropService.getHomeCrops(me.getId()));
    }

    // 작물 재배 상태 변경
    @PatchMapping(value = "/{cropId:\\d+}")
    public ResponseEntity<?> updateHarvest(
            @AuthenticationPrincipal CustomUserDetails me,
            @PathVariable Integer cropId,
            @RequestBody HarvestStatusFormDto harvestStatusFormDto
    ) throws IOException {
        // 권한이 없을 때
        if (me == null) return ResponseEntity.status(401).build();

        // 재배 상태 업데이트
        cropService.updateHarvest(cropId, me.getId(), harvestStatusFormDto.getHarvestStatus());

        // 성공 json 반환
        return ResponseEntity.ok(new ApiSuccess(200, "성공적으로 처리되었습니다."));
    }

    // 작물 수정
    @PutMapping(value = "/{cropId:\\d+}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateCrop(
            @AuthenticationPrincipal CustomUserDetails me,
            @PathVariable Integer cropId,
            @RequestPart("form") CropUpdateFormDto cropUpdateFormDto,
            @RequestPart("image") MultipartFile image
    ) throws IOException {
        // 권한이 없을 떄
        if (me == null) return ResponseEntity.status(401).build();

        // 작물 수정
        cropService.updateCrop(cropId, me.getId(), cropUpdateFormDto, image);

        // 성공 json 반환
        return ResponseEntity.ok(new ApiSuccess(200, "성공적으로 처리되었습니다."));
    }
}
