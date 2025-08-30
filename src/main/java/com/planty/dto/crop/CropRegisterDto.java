package com.planty.dto.crop;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDate;


// CropRegisterFormDto와 이미지 파일 결합
@Getter @Setter
public class CropRegisterDto {
    private String cropName;
    private LocalDate startAt;
    private LocalDate endAt;
    private String howTo;
    private String environment;
    private String temperature;
    private String height;
    private MultipartFile imageFile;
}
