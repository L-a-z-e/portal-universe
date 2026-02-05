package com.portal.universe.blogservice.file.service;

import com.portal.universe.blogservice.common.exception.BlogErrorCode;
import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Utilities;
import software.amazon.awssdk.services.s3.model.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
@DisplayName("FileService 테스트")
class FileServiceTest {

    @Mock
    private S3Client s3Client;

    @InjectMocks
    private FileService fileService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(fileService, "bucketName", "test-bucket");
    }

    @Nested
    @DisplayName("uploadFile 메서드")
    class UploadFileTests {

        @Test
        @DisplayName("should_uploadFile_and_returnUrl")
        void should_uploadFile_and_returnUrl() throws Exception {
            // given
            MultipartFile mockFile = mock(MultipartFile.class);
            when(mockFile.isEmpty()).thenReturn(false);
            when(mockFile.getSize()).thenReturn(1024L);
            when(mockFile.getOriginalFilename()).thenReturn("test.jpg");
            when(mockFile.getContentType()).thenReturn("image/jpeg");
            when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[1024]));

            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                    .thenReturn(PutObjectResponse.builder().build());

            S3Utilities utilities = mock(S3Utilities.class);
            when(s3Client.utilities()).thenReturn(utilities);
            when(utilities.getUrl(any(Consumer.class)))
                    .thenReturn(new URL("https://s3.example.com/test-bucket/file.jpg"));

            // when
            String result = fileService.uploadFile(mockFile);

            // then
            assertThat(result).contains("https://s3.example.com");
            verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        }

        @Test
        @DisplayName("should_throwException_when_fileIsEmpty")
        void should_throwException_when_fileIsEmpty() {
            // given
            MultipartFile mockFile = mock(MultipartFile.class);
            when(mockFile.isEmpty()).thenReturn(true);

            // when & then
            assertThatThrownBy(() -> fileService.uploadFile(mockFile))
                    .isInstanceOf(CustomBusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", BlogErrorCode.FILE_EMPTY);
            verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        }

        @Test
        @DisplayName("should_throwException_when_sizeExceeded")
        void should_throwException_when_sizeExceeded() {
            // given
            MultipartFile mockFile = mock(MultipartFile.class);
            when(mockFile.isEmpty()).thenReturn(false);
            when(mockFile.getSize()).thenReturn(101L * 1024 * 1024); // 101 MB (exceeds 100 MB limit)
            // getOriginalFilename은 size 체크에서 예외가 발생하므로 호출되지 않음

            // when & then - FileService validates size before extension
            assertThatThrownBy(() -> fileService.uploadFile(mockFile))
                    .isInstanceOf(CustomBusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", BlogErrorCode.FILE_SIZE_EXCEEDED);
            verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        }

        @Test
        @DisplayName("should_throwException_when_invalidExtension")
        void should_throwException_when_invalidExtension() {
            // given
            MultipartFile mockFile = mock(MultipartFile.class);
            when(mockFile.isEmpty()).thenReturn(false);
            when(mockFile.getSize()).thenReturn(1024L);
            when(mockFile.getOriginalFilename()).thenReturn("test.exe");

            // when & then
            assertThatThrownBy(() -> fileService.uploadFile(mockFile))
                    .isInstanceOf(CustomBusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", BlogErrorCode.FILE_TYPE_NOT_ALLOWED);
            verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        }

        @Test
        @DisplayName("should_throwException_when_s3Fails")
        void should_throwException_when_s3Fails() throws IOException {
            // given
            MultipartFile mockFile = mock(MultipartFile.class);
            when(mockFile.isEmpty()).thenReturn(false);
            when(mockFile.getSize()).thenReturn(1024L);
            when(mockFile.getOriginalFilename()).thenReturn("test.jpg");
            when(mockFile.getContentType()).thenReturn("image/jpeg");
            when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[1024]));

            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                    .thenThrow(S3Exception.builder().message("S3 error").build());

            // when & then
            assertThatThrownBy(() -> fileService.uploadFile(mockFile))
                    .isInstanceOf(CustomBusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", BlogErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    @Nested
    @DisplayName("deleteFile 메서드")
    class DeleteFileTests {

        @Test
        @DisplayName("should_deleteFile_from_s3")
        void should_deleteFile_from_s3() {
            // given
            String fileUrl = "https://s3.example.com/test-bucket/files/user1/test.jpg";

            when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                    .thenReturn(DeleteObjectResponse.builder().build());

            // when
            fileService.deleteFile(fileUrl);

            // then
            verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
        }

        @Test
        @DisplayName("should_throwException_when_invalidUrl")
        void should_throwException_when_invalidUrl() {
            // given
            String invalidUrl = "https://example.com/file.jpg";

            // when & then
            assertThatThrownBy(() -> fileService.deleteFile(invalidUrl))
                    .isInstanceOf(CustomBusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", BlogErrorCode.INVALID_FILE_URL);
            verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
        }
    }
}
