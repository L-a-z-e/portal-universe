package com.portal.universe.blogservice.file.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 파일 업로드 응답 DTO
 */
@Getter
@Builder
public class FileUploadResponse {
    /**
     * S3 파일 접근 URL
     */
    private String url;

    /**
     * 원본 파일명
     */
    private String filename;

    /**
     * 파일 크기 (bytes)
     */
    private Long size;

    /**
     * 파일 MIME 타입
     */
    private String contentType;
}