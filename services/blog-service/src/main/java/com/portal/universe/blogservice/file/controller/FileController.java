package com.portal.universe.blogservice.file.controller;

import com.portal.universe.blogservice.file.dto.FileDeleteRequest;
import com.portal.universe.blogservice.file.dto.FileUploadResponse;
import com.portal.universe.blogservice.file.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 파일 업로드/관리 API 컨트롤러
 * Gateway 라우팅: /api/blog/file/** -> blog-service/file/**
 */
@Slf4j
@Tag(name = "File", description = "파일 업로드/관리 API")
@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    /**
     * 파일 업로드 API
     * - 이미지, 첨부파일 등 다양한 파일 업로드 지원
     * - Gateway를 통한 요청: POST /api/blog/file/upload
     */
    @Operation(
            summary = "파일 업로드",
            description = "S3에 파일을 업로드하고 접근 URL을 반환합니다. 인증된 사용자만 사용 가능합니다."
    )
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FileUploadResponse> uploadFile(
            @RequestParam("file") MultipartFile file
    ) {
        log.info("📤 파일 업로드 요청 - 파일명: {}, 크기: {}bytes",
                file.getOriginalFilename(), file.getSize());

        String url = fileService.uploadFile(file);

        FileUploadResponse response = FileUploadResponse.builder()
                .url(url)
                .filename(file.getOriginalFilename())
                .size(file.getSize())
                .contentType(file.getContentType())
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * 파일 삭제 API
     * - S3에서 파일 삭제
     * - Gateway를 통한 요청: DELETE /api/blog/file/delete
     */
    @Operation(
            summary = "파일 삭제",
            description = "S3에서 파일을 삭제합니다. ADMIN 권한이 필요합니다."
    )
    @DeleteMapping("/delete")
    @PreAuthorize("hasAnyAuthority('ROLE_BLOG_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<Void> deleteFile(@RequestBody FileDeleteRequest request) {
        log.info("🗑️ 파일 삭제 요청 - URL: {}", request.getUrl());

        fileService.deleteFile(request.getUrl());

        return ResponseEntity.noContent().build();
    }
}