package com.portal.universe.blogservice.file.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PresignedUrlRequest {

    @NotBlank(message = "파일명은 필수입니다")
    private String filename;

    @NotBlank(message = "Content-Type은 필수입니다")
    private String contentType;

    @Positive(message = "파일 크기는 0보다 커야 합니다")
    private long contentLength;
}
