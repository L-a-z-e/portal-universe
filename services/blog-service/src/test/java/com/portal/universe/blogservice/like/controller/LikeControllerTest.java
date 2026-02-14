package com.portal.universe.blogservice.like.controller;

import com.portal.universe.blogservice.like.dto.LikeStatusResponse;
import com.portal.universe.blogservice.like.dto.LikeToggleResponse;
import com.portal.universe.blogservice.like.dto.LikerResponse;
import com.portal.universe.blogservice.like.service.LikeService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LikeController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(AuthUserWebConfig.class)
@DisplayName("LikeController 테스트")
class LikeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LikeService likeService;

    private AuthUser authUser;

    @BeforeEach
    void setUp() {
        authUser = new AuthUser("user-1", "User Name", "UserNick", null);
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    @DisplayName("POST /posts/{postId}/like - should_toggleLike")
    void should_toggleLike() throws Exception {
        // given
        LikeToggleResponse response = LikeToggleResponse.of(true, 100L);
        given(likeService.toggleLike(eq("post-1"), eq("user-1"), anyString())).willReturn(response);

        // when & then
        mockMvc.perform(post("/posts/post-1/like")
                .requestAttr("authUser", authUser))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.liked").value(true))
            .andExpect(jsonPath("$.data.likeCount").value(100));

        verify(likeService).toggleLike(eq("post-1"), eq("user-1"), anyString());
    }

    @Test
    @DisplayName("GET /posts/{postId}/like - should_returnLikeStatus")
    void should_returnLikeStatus() throws Exception {
        // given
        LikeStatusResponse response = LikeStatusResponse.of(true, 50L);
        given(likeService.getLikeStatus("post-1", "user-1")).willReturn(response);

        // when & then
        mockMvc.perform(get("/posts/post-1/like")
                .requestAttr("authUser", authUser))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.liked").value(true))
            .andExpect(jsonPath("$.data.likeCount").value(50));

        verify(likeService).getLikeStatus("post-1", "user-1");
    }

    @Test
    @DisplayName("GET /posts/{postId}/likes - should_returnLikers")
    void should_returnLikers() throws Exception {
        // given
        LikerResponse likerResponse = new LikerResponse(
            "user-2",
            "User Two",
            LocalDateTime.now()
        );
        Page<LikerResponse> page = new PageImpl<>(List.of(likerResponse));
        given(likeService.getLikers(eq("post-1"), any(Pageable.class))).willReturn(page);

        // when & then
        mockMvc.perform(get("/posts/post-1/likes"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.items[0].userId").value("user-2"))
            .andExpect(jsonPath("$.data.items[0].userName").value("User Two"));

        verify(likeService).getLikers(eq("post-1"), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /posts/{postId}/likes - should_returnEmptyPage_when_noLikers")
    void should_returnEmptyPage_when_noLikers() throws Exception {
        // given
        Page<LikerResponse> emptyPage = Page.empty();
        given(likeService.getLikers(eq("post-1"), any(Pageable.class))).willReturn(emptyPage);

        // when & then
        mockMvc.perform(get("/posts/post-1/likes"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.items").isEmpty())
            .andExpect(jsonPath("$.data.totalElements").value(0));

        verify(likeService).getLikers(eq("post-1"), any(Pageable.class));
    }
}
