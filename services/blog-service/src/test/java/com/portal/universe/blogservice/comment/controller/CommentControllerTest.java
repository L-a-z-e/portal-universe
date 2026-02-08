package com.portal.universe.blogservice.comment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.universe.blogservice.comment.dto.CommentCreateRequest;
import com.portal.universe.blogservice.comment.dto.CommentResponse;
import com.portal.universe.blogservice.comment.dto.CommentUpdateRequest;
import com.portal.universe.blogservice.comment.service.CommentService;
import com.portal.universe.commonlibrary.security.config.AuthUserWebConfig;
import com.portal.universe.commonlibrary.security.context.AuthUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CommentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(AuthUserWebConfig.class)
@DisplayName("CommentController 테스트")
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CommentService commentService;

    private AuthUser authUser;
    private CommentResponse commentResponse;

    @BeforeEach
    void setUp() {
        authUser = new AuthUser("user-1", "User Name", "UserNick");

        commentResponse = new CommentResponse(
            "comment-1",
            "post-1",
            "user-1",
            "UserNick",
            "Test Comment Content",
            null,
            10L,
            false,
            LocalDateTime.now(),
            LocalDateTime.now()
        );
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    @DisplayName("POST /comments - should_createComment_when_validRequest")
    void should_createComment_when_validRequest() throws Exception {
        // given
        CommentCreateRequest request = new CommentCreateRequest(
            "post-1",
            null,
            "Test Comment Content"
        );
        given(commentService.createComment(any(CommentCreateRequest.class), eq("user-1"), anyString()))
            .willReturn(commentResponse);

        // when & then
        mockMvc.perform(post("/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .requestAttr("authUser", authUser))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value("comment-1"))
            .andExpect(jsonPath("$.data.content").value("Test Comment Content"));

        verify(commentService).createComment(any(CommentCreateRequest.class), eq("user-1"), anyString());
    }

    @Test
    @DisplayName("POST /comments - should_returnBadRequest_when_contentIsBlank")
    void should_returnBadRequest_when_contentIsBlank() throws Exception {
        // given
        CommentCreateRequest request = new CommentCreateRequest(
            "post-1",
            null,
            ""
        );

        // when & then
        mockMvc.perform(post("/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .requestAttr("authUser", authUser))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /comments - should_returnBadRequest_when_postIdIsBlank")
    void should_returnBadRequest_when_postIdIsBlank() throws Exception {
        // given
        CommentCreateRequest request = new CommentCreateRequest(
            "",
            null,
            "Test Comment"
        );

        // when & then
        mockMvc.perform(post("/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .requestAttr("authUser", authUser))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /comments/{commentId} - should_updateComment")
    void should_updateComment() throws Exception {
        // given
        CommentUpdateRequest request = new CommentUpdateRequest("Updated Comment Content");
        given(commentService.updateComment(eq("comment-1"), any(CommentUpdateRequest.class), eq("user-1")))
            .willReturn(commentResponse);

        // when & then
        mockMvc.perform(put("/comments/comment-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .requestAttr("authUser", authUser))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value("comment-1"));

        verify(commentService).updateComment(eq("comment-1"), any(CommentUpdateRequest.class), eq("user-1"));
    }

    @Test
    @DisplayName("DELETE /comments/{commentId} - should_deleteComment")
    void should_deleteComment() throws Exception {
        // when & then
        mockMvc.perform(delete("/comments/comment-1")
                .requestAttr("authUser", authUser))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").doesNotExist());

        verify(commentService).deleteComment("comment-1", "user-1");
    }

    @Test
    @DisplayName("GET /comments/post/{postId} - should_returnComments")
    void should_returnComments() throws Exception {
        // given
        given(commentService.getCommentsByPostId("post-1")).willReturn(List.of(commentResponse));

        // when & then
        mockMvc.perform(get("/comments/post/post-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].id").value("comment-1"))
            .andExpect(jsonPath("$.data[0].postId").value("post-1"));

        verify(commentService).getCommentsByPostId("post-1");
    }
}
