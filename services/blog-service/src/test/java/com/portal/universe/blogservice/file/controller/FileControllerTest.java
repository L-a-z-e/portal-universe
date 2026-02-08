package com.portal.universe.blogservice.file.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.universe.blogservice.file.dto.FileDeleteRequest;
import com.portal.universe.blogservice.file.service.FileService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FileController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FileController 테스트")
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FileService fileService;

    @Test
    @DisplayName("POST /file/upload - should_uploadFile")
    void should_uploadFile() throws Exception {
        // given
        MockMultipartFile mockFile = new MockMultipartFile(
            "file",
            "test.jpg",
            "image/jpeg",
            new byte[1024]
        );
        String fileUrl = "https://s3.amazonaws.com/bucket/test.jpg";
        given(fileService.uploadFile(any())).willReturn(fileUrl);

        // when & then
        mockMvc.perform(multipart("/file/upload")
                .file(mockFile))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.url").value(fileUrl))
            .andExpect(jsonPath("$.data.filename").value("test.jpg"))
            .andExpect(jsonPath("$.data.size").value(1024))
            .andExpect(jsonPath("$.data.contentType").value("image/jpeg"));

        verify(fileService).uploadFile(any());
    }

    @Test
    @DisplayName("POST /file/upload - should_returnBadRequest_when_noFile")
    void should_returnBadRequest_when_noFile() throws Exception {
        // given - Create an empty MockMultipartFile
        MockMultipartFile emptyFile = new MockMultipartFile(
            "file",
            "",
            "application/octet-stream",
            new byte[0]
        );

        // FileService가 빈 파일에 대해 예외를 던지도록 mock 설정
        given(fileService.uploadFile(any())).willThrow(
            new com.portal.universe.commonlibrary.exception.CustomBusinessException(
                com.portal.universe.blogservice.common.exception.BlogErrorCode.FILE_EMPTY
            )
        );

        // when & then
        mockMvc.perform(multipart("/file/upload")
                .file(emptyFile))
            .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("DELETE /file/delete - should_deleteFile")
    void should_deleteFile() throws Exception {
        // given
        FileDeleteRequest request = new FileDeleteRequest();
        ReflectionTestUtils.setField(request, "url", "https://s3.amazonaws.com/bucket/test.jpg");

        // when & then
        mockMvc.perform(delete("/file/delete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        verify(fileService).deleteFile("https://s3.amazonaws.com/bucket/test.jpg");
    }

    @Test
    @DisplayName("DELETE /file/delete - should_returnBadRequest_when_urlIsBlank")
    void should_returnBadRequest_when_urlIsBlank() throws Exception {
        // given
        FileDeleteRequest request = new FileDeleteRequest();
        ReflectionTestUtils.setField(request, "url", "");

        // when & then - ValidationException will return 400
        mockMvc.perform(delete("/file/delete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().is4xxClientError());
    }
}
