package com.planty.dto.crop;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDate;


// CropAnalysisFormDto와 이미지 파일 결합
@Getter @Setter
public class CropAnalysisDto {
    private String cropName;
    private LocalDate startAt;
    private LocalDate endAt;
    private MultipartFile imageFile;
}
