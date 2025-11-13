package com.portal.universe.blogservice.file.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 파일 삭제 요청 DTO
 */
@Getter
@NoArgsConstructor
public class FileDeleteRequest {
    /**
     * 삭제할 파일의 S3 URL
     */
    private String url;
}