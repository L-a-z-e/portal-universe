package com.portal.universe.blogservice.series.controller;

import com.portal.universe.blogservice.series.dto.*;
import com.portal.universe.blogservice.series.service.SeriesService;
import com.portal.universe.commonlibrary.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 시리즈 API Controller
 */
@RestController
@RequestMapping("/series")
@RequiredArgsConstructor
public class SeriesController {

    private final SeriesService seriesService;

    /**
     * 시리즈 생성
     * POST /api/series
     */
    @PostMapping
    public ApiResponse<SeriesResponse> createSeries(
            @Valid @RequestBody SeriesCreateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String authorId = jwt.getSubject();
        String authorName = jwt.getClaim("name");
        SeriesResponse response = seriesService.createSeries(request, authorId, authorName);
        return ApiResponse.success(response);
    }

    /**
     * 시리즈 수정
     * PUT /api/series/{seriesId}
     */
    @PutMapping("/{seriesId}")
    public ApiResponse<SeriesResponse> updateSeries(
            @PathVariable String seriesId,
            @Valid @RequestBody SeriesUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String authorId = jwt.getSubject();
        SeriesResponse response = seriesService.updateSeries(seriesId, request, authorId);
        return ApiResponse.success(response);
    }

    /**
     * 시리즈 삭제
     * DELETE /api/series/{seriesId}
     */
    @DeleteMapping("/{seriesId}")
    public ApiResponse<Void> deleteSeries(
            @PathVariable String seriesId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String authorId = jwt.getSubject();
        seriesService.deleteSeries(seriesId, authorId);
        return ApiResponse.success(null);
    }

    /**
     * 시리즈 상세 조회
     * GET /api/series/{seriesId}
     */
    @GetMapping("/{seriesId}")
    public ApiResponse<SeriesResponse> getSeriesById(@PathVariable String seriesId) {
        SeriesResponse response = seriesService.getSeriesById(seriesId);
        return ApiResponse.success(response);
    }

    /**
     * 작성자별 시리즈 목록 조회
     * GET /api/series/author/{authorId}
     */
    @GetMapping("/author/{authorId}")
    public ApiResponse<List<SeriesListResponse>> getSeriesByAuthor(@PathVariable String authorId) {
        List<SeriesListResponse> responses = seriesService.getSeriesByAuthor(authorId);
        return ApiResponse.success(responses);
    }

    /**
     * 내 시리즈 목록 조회
     * GET /api/series/my
     */
    @GetMapping("/my")
    public ApiResponse<List<SeriesListResponse>> getMySeries(@AuthenticationPrincipal Jwt jwt) {
        String authorId = jwt.getSubject();
        List<SeriesListResponse> responses = seriesService.getSeriesByAuthor(authorId);
        return ApiResponse.success(responses);
    }

    /**
     * 시리즈에 포스트 추가
     * POST /api/series/{seriesId}/posts/{postId}
     */
    @PostMapping("/{seriesId}/posts/{postId}")
    public ApiResponse<SeriesResponse> addPostToSeries(
            @PathVariable String seriesId,
            @PathVariable String postId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String authorId = jwt.getSubject();
        SeriesResponse response = seriesService.addPostToSeries(seriesId, postId, authorId);
        return ApiResponse.success(response);
    }

    /**
     * 시리즈에서 포스트 제거
     * DELETE /api/series/{seriesId}/posts/{postId}
     */
    @DeleteMapping("/{seriesId}/posts/{postId}")
    public ApiResponse<SeriesResponse> removePostFromSeries(
            @PathVariable String seriesId,
            @PathVariable String postId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String authorId = jwt.getSubject();
        SeriesResponse response = seriesService.removePostFromSeries(seriesId, postId, authorId);
        return ApiResponse.success(response);
    }

    /**
     * 시리즈 내 포스트 순서 변경
     * PUT /api/series/{seriesId}/posts/order
     */
    @PutMapping("/{seriesId}/posts/order")
    public ApiResponse<SeriesResponse> reorderPosts(
            @PathVariable String seriesId,
            @Valid @RequestBody SeriesPostOrderRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String authorId = jwt.getSubject();
        SeriesResponse response = seriesService.reorderPosts(seriesId, request, authorId);
        return ApiResponse.success(response);
    }

    /**
     * 특정 포스트가 포함된 시리즈 조회
     * GET /api/series/by-post/{postId}
     */
    @GetMapping("/by-post/{postId}")
    public ApiResponse<List<SeriesListResponse>> getSeriesByPostId(@PathVariable String postId) {
        List<SeriesListResponse> responses = seriesService.getSeriesByPostId(postId);
        return ApiResponse.success(responses);
    }
}