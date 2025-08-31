package com.planty.controller.diary;


import com.planty.config.CustomUserDetails;
import com.planty.dto.diary.DiaryAnalysisDto;
import com.planty.dto.diary.DiaryAnalysisFormDto;
import com.planty.dto.diary.DiaryAnalysisResDto;
import com.planty.service.crop.CropService;
import com.planty.service.diary.DiaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

// 재배 일지
@RestController
@RequestMapping("/api/diary")
@RequiredArgsConstructor
public class DiaryController {

    private final DiaryService diaryService;

    // 재배 일지 이미지 분석
    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DiaryAnalysisResDto> analyze(
            @AuthenticationPrincipal CustomUserDetails me,
            @RequestPart("form") DiaryAnalysisFormDto form,
            @RequestPart("image") MultipartFile image
    ) {
        // 권한이 없을 때
        if (me == null) return ResponseEntity.status(401).build();

        // 서비스가 기대하는 기존 DTO로 합쳐져 전달
        DiaryAnalysisDto dto = new DiaryAnalysisDto();
        dto.setCropName(form.getCropName());
        dto.setPromptKey(form.getPromptKey());
        dto.setImage(image);
        dto.setCropId(form.getCropId());

        // 반환
        return ResponseEntity.ok(diaryService.diaryAnalysis(me.getId(), dto));
    }

}
