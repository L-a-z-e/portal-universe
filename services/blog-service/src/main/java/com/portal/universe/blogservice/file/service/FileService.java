package com.portal.universe.blogservice.file.service;

import com.portal.universe.blogservice.exception.BlogErrorCode;
import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 파일 업로드/관리 서비스 (AWS S3)
 * - 이미지, 첨부파일 등 다양한 파일 타입 지원
 * - LocalStack 및 실제 AWS S3 환경 지원
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    // 허용 가능한 이미지 확장자 목록
    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = Arrays.asList(
            "jpg", "jpeg", "png", "gif", "webp", "svg"
    );

    // 최대 파일 크기 (100MB)
    private static final long MAX_FILE_SIZE = 100 * 1024 * 1024;

    /**
     * 애플리케이션 시작 시 S3 버킷 존재 확인 및 자동 생성
     */
    @PostConstruct
    public void ensureBucketExists() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder()
                    .bucket(bucketName)
                    .build());
            log.info("✅ S3 버킷 존재 확인: {}", bucketName);
        } catch (NoSuchBucketException e) {
            log.warn("⚠️ S3 버킷이 존재하지 않습니다. 생성 중: {}", bucketName);
            s3Client.createBucket(CreateBucketRequest.builder()
                    .bucket(bucketName)
                    .build());
            log.info("✅ S3 버킷 생성 완료: {}", bucketName);
        } catch (Exception e) {
            log.error("❌ S3 버킷 확인 중 오류 발생: {}", e.getMessage());
        }
    }

    /**
     * 파일을 S3에 업로드하고 접근 URL 반환
     *
     * @param file 업로드할 파일
     * @return S3 파일 접근 URL
     * @throws FileUploadException 파일 업로드 실패 시
     */
    public String uploadFile(MultipartFile file) {
        validateFile(file);

        String key = generateUniqueKey(file.getOriginalFilename());

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(
                    putObjectRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );

            String url = s3Client.utilities()
                    .getUrl(builder -> builder.bucket(bucketName).key(key))
                    .toString();

            log.info("✅ 파일 업로드 완료 - Key: {}, URL: {}", key, url);
            return url;

        } catch (IOException e) {
            log.error("❌ 파일 읽기 실패: {}", e.getMessage());
            throw new CustomBusinessException(BlogErrorCode.FILE_UPLOAD_FAILED);
        } catch (S3Exception e) {
            log.error("❌ S3 업로드 실패: {}", e.getMessage());
            throw new CustomBusinessException(BlogErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    /**
     * S3에서 파일 삭제
     *
     * @param fileUrl 삭제할 파일의 S3 URL
     */
    public void deleteFile(String fileUrl) {
        try {
            String key = extractKeyFromUrl(fileUrl);

            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("✅ 파일 삭제 완료 - Key: {}", key);

        } catch (S3Exception e) {
            log.error("❌ S3 파일 삭제 실패: {}", e.getMessage());
            throw new CustomBusinessException(BlogErrorCode.FILE_DELETE_FAILED);
        }
    }

    /**
     * 파일 유효성 검증
     * - 파일 크기 제한
     * - 확장자 검증 (이미지만 허용하려면 주석 해제)
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new CustomBusinessException(BlogErrorCode.FILE_EMPTY);
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new CustomBusinessException(BlogErrorCode.FILE_SIZE_EXCEEDED);
        }

        String extension = getFileExtension(file.getOriginalFilename());

        // 이미지만 허용하려면 아래 주석 해제
        // if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension.toLowerCase())) {
        //     throw new FileUploadException("허용되지 않는 파일 형식입니다: " + extension);
        // }
    }

    /**
     * 파일명 중복 방지를 위한 고유 키 생성
     * 형식: UUID_원본파일명
     */
    private String generateUniqueKey(String originalFilename) {
        return UUID.randomUUID() + "_" + originalFilename;
    }

    /**
     * S3 URL에서 객체 키(key) 추출
     * URL 형식: http://localhost:4566/blog-bucket/uuid_filename.jpg
     */
    private String extractKeyFromUrl(String url) {
        String[] parts = url.split(bucketName + "/");
        if (parts.length < 2) {
            throw new CustomBusinessException(BlogErrorCode.INVALID_FILE_URL);
        }
        return parts[1];
    }

    /**
     * 파일 확장자 추출
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
}