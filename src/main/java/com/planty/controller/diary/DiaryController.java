package com.planty.controller.diary;


import com.planty.common.ApiSuccess;
import com.planty.config.CustomUserDetails;
import com.planty.dto.diary.*;
import com.planty.service.crop.CropService;
import com.planty.service.diary.DiaryService;
import com.planty.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// 재배 일지
@RestController
@RequestMapping("/api/diary")
@RequiredArgsConstructor
public class DiaryController {

    private final DiaryService diaryService;
    private final StorageService storageService;

    // 재배 일지 이미지 분석
    @PostMapping(value = "/analyze/{cropId:\\d+}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DiaryAnalysisResDto> analyze(
            @AuthenticationPrincipal CustomUserDetails me,
            @RequestPart("form") DiaryAnalysisFormDto form,
            @RequestPart("image") MultipartFile image,
            @PathVariable Integer cropId
    ) {
        // 권한이 없을 때
        if (me == null) return ResponseEntity.status(401).build();

        // 서비스가 기대하는 기존 DTO로 합쳐져 전달
        DiaryAnalysisDto dto = new DiaryAnalysisDto();
        dto.setCropName(form.getCropName());
        dto.setPromptKey(form.getPromptKey());
        dto.setImage(image);
        dto.setCropId(cropId);

        // 반환
        return ResponseEntity.ok(diaryService.diaryAnalysis(me.getId(), dto));
    }

    // 재배 일지 작성
    @PostMapping(value = "create/{cropId:\\d+}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> create(
            @AuthenticationPrincipal CustomUserDetails me,
            @RequestPart("form")DiaryFormDto form,
            @RequestPart("image")List<MultipartFile> images,
            @PathVariable Integer cropId
    ) throws IOException {
        // 권한이 없을 때
        if (me == null) return ResponseEntity.status(401).build();

        // 파일 저장 -> URL 리스트 생성
        List<String> urls = new ArrayList<>();
        if (images != null) {
            for (MultipartFile f : images) {
                if (!f.isEmpty()) {
                    urls.add(storageService.save(f, "diary"));
                }
            }
        }

        // DiaryDto로 변환
        DiaryDto dto = new DiaryDto();
        dto.setTitle(form.getTitle());
        dto.setContent(form.getContent());
        dto.setImageUrls(urls);

        // AI 분석 없이 재배 일지를 등록할 경우
        if(form.getAnalysis() != null) dto.setAnalysis(form.getAnalysis());

        // 서비스 호출
        diaryService.saveDiary(me.getId(), dto, cropId);

        // 반환
        return ResponseEntity.status(201).body(new ApiSuccess(201, "성공적으로 처리되었습니다."));
    }
}
