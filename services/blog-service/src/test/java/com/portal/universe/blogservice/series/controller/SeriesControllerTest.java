package com.portal.universe.blogservice.series.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.universe.blogservice.post.dto.PostSummaryResponse;
import com.portal.universe.blogservice.series.dto.*;
import com.portal.universe.blogservice.series.service.SeriesService;
import com.portal.universe.commonlibrary.security.config.GatewayUserWebConfig;
import com.portal.universe.commonlibrary.security.context.GatewayUser;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SeriesController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GatewayUserWebConfig.class)
@DisplayName("SeriesController 테스트")
class SeriesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SeriesService seriesService;

    private GatewayUser gatewayUser;
    private SeriesResponse seriesResponse;
    private PostSummaryResponse postSummaryResponse;

    @BeforeEach
    void setUp() {
        gatewayUser = new GatewayUser("user-1", "User Name", "UserNick");

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("user-1", null, List.of())
        );

        seriesResponse = new SeriesResponse(
            "series-1",
            "Test Series",
            "Test Description",
            "user-1",
            "UserNick",
            "thumbnail.jpg",
            List.of("post-1", "post-2"),
            2,
            LocalDateTime.now(),
            LocalDateTime.now()
        );

        postSummaryResponse = new PostSummaryResponse(
            "post-1",
            "Post Title",
            "Post Summary",
            "user-1",
            "UserNick",
            Set.of("tag1"),
            "Technology",
            "thumbnail.jpg",
            List.of(),
            100L,
            50L,
            10L,
            LocalDateTime.now(),
            5
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("POST /series - should_createSeries_when_validRequest")
    void should_createSeries_when_validRequest() throws Exception {
        // given
        SeriesCreateRequest request = new SeriesCreateRequest(
            "Test Series",
            "Test Description",
            "thumbnail.jpg"
        );
        given(seriesService.createSeries(any(SeriesCreateRequest.class), eq("user-1"), anyString()))
            .willReturn(seriesResponse);

        // when & then
        mockMvc.perform(post("/series")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .requestAttr("gatewayUser", gatewayUser))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value("series-1"))
            .andExpect(jsonPath("$.data.name").value("Test Series"));

        verify(seriesService).createSeries(any(SeriesCreateRequest.class), eq("user-1"), anyString());
    }

    @Test
    @DisplayName("POST /series - should_returnBadRequest_when_nameIsBlank")
    void should_returnBadRequest_when_nameIsBlank() throws Exception {
        // given
        SeriesCreateRequest request = new SeriesCreateRequest(
            "",
            "Test Description",
            null
        );

        // when & then
        mockMvc.perform(post("/series")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .requestAttr("gatewayUser", gatewayUser))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /series/{seriesId} - should_updateSeries")
    void should_updateSeries() throws Exception {
        // given
        SeriesUpdateRequest request = new SeriesUpdateRequest(
            "Updated Series",
            "Updated Description",
            "new-thumbnail.jpg"
        );
        given(seriesService.updateSeries(eq("series-1"), any(SeriesUpdateRequest.class), eq("user-1")))
            .willReturn(seriesResponse);

        // when & then
        mockMvc.perform(put("/series/series-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value("series-1"));

        verify(seriesService).updateSeries(eq("series-1"), any(SeriesUpdateRequest.class), eq("user-1"));
    }

    @Test
    @DisplayName("DELETE /series/{seriesId} - should_deleteSeries")
    void should_deleteSeries() throws Exception {
        // when & then
        mockMvc.perform(delete("/series/series-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").doesNotExist());

        verify(seriesService).deleteSeries("series-1", "user-1");
    }

    @Test
    @DisplayName("GET /series/{seriesId} - should_returnSeries")
    void should_returnSeries() throws Exception {
        // given
        given(seriesService.getSeriesById("series-1")).willReturn(seriesResponse);

        // when & then
        mockMvc.perform(get("/series/series-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value("series-1"))
            .andExpect(jsonPath("$.data.name").value("Test Series"));

        verify(seriesService).getSeriesById("series-1");
    }

    @Test
    @DisplayName("GET /series/author/{authorId} - should_returnAuthorSeries")
    void should_returnAuthorSeries() throws Exception {
        // given
        SeriesListResponse listResponse = new SeriesListResponse(
            "series-1",
            "Test Series",
            "Test Description",
            "user-1",
            "UserNick",
            "thumbnail.jpg",
            2,
            LocalDateTime.now(),
            LocalDateTime.now()
        );
        given(seriesService.getSeriesByAuthor("user-1")).willReturn(List.of(listResponse));

        // when & then
        mockMvc.perform(get("/series/author/user-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].id").value("series-1"))
            .andExpect(jsonPath("$.data[0].authorId").value("user-1"));

        verify(seriesService).getSeriesByAuthor("user-1");
    }

    @Test
    @DisplayName("GET /series/{seriesId}/posts - should_returnSeriesPosts")
    void should_returnSeriesPosts() throws Exception {
        // given
        given(seriesService.getSeriesPosts("series-1")).willReturn(List.of(postSummaryResponse));

        // when & then
        mockMvc.perform(get("/series/series-1/posts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].id").value("post-1"))
            .andExpect(jsonPath("$.data[0].title").value("Post Title"));

        verify(seriesService).getSeriesPosts("series-1");
    }

    @Test
    @DisplayName("POST /series/{seriesId}/posts/{postId} - should_addPost")
    void should_addPost() throws Exception {
        // given
        given(seriesService.addPostToSeries("series-1", "post-1", "user-1")).willReturn(seriesResponse);

        // when & then
        mockMvc.perform(post("/series/series-1/posts/post-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value("series-1"));

        verify(seriesService).addPostToSeries("series-1", "post-1", "user-1");
    }

    @Test
    @DisplayName("DELETE /series/{seriesId}/posts/{postId} - should_removePost")
    void should_removePost() throws Exception {
        // given
        given(seriesService.removePostFromSeries("series-1", "post-1", "user-1")).willReturn(seriesResponse);

        // when & then
        mockMvc.perform(delete("/series/series-1/posts/post-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value("series-1"));

        verify(seriesService).removePostFromSeries("series-1", "post-1", "user-1");
    }

    @Test
    @DisplayName("PUT /series/{seriesId}/posts/order - should_reorderPosts")
    void should_reorderPosts() throws Exception {
        // given
        SeriesPostOrderRequest request = new SeriesPostOrderRequest(List.of("post-2", "post-1"));
        given(seriesService.reorderPosts(eq("series-1"), any(SeriesPostOrderRequest.class), eq("user-1")))
            .willReturn(seriesResponse);

        // when & then
        mockMvc.perform(put("/series/series-1/posts/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value("series-1"));

        verify(seriesService).reorderPosts(eq("series-1"), any(SeriesPostOrderRequest.class), eq("user-1"));
    }
}
