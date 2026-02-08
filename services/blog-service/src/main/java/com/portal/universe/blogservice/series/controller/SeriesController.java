package com.portal.universe.blogservice.series.controller;

import com.portal.universe.blogservice.post.dto.PostSummaryResponse;
import com.portal.universe.blogservice.series.dto.*;
import com.portal.universe.blogservice.series.service.SeriesService;
import com.portal.universe.commonlibrary.response.ApiResponse;
import com.portal.universe.commonlibrary.security.context.AuthUser;
import com.portal.universe.commonlibrary.security.context.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@Tag(name = "Series", description = "시리즈(연재 모음) 관리 API")
@RestController
@RequestMapping("/series")
@RequiredArgsConstructor
public class SeriesController {

    private final SeriesService seriesService;

    @Operation(summary = "시리즈 생성")
    @PostMapping
    public ApiResponse<SeriesResponse> createSeries(
            @Valid @RequestBody SeriesCreateRequest request,
            @CurrentUser AuthUser user
    ) {
        SeriesResponse response = seriesService.createSeries(request, user.uuid(), user.nickname());
        return ApiResponse.success(response);
    }

    @Operation(summary = "시리즈 수정")
    @PutMapping("/{seriesId}")
    public ApiResponse<SeriesResponse> updateSeries(
            @Parameter(description = "시리즈 ID") @PathVariable String seriesId,
            @Valid @RequestBody SeriesUpdateRequest request,
            @CurrentUser AuthUser user
    ) {
        SeriesResponse response = seriesService.updateSeries(seriesId, request, user.uuid());
        return ApiResponse.success(response);
    }

    @Operation(summary = "시리즈 삭제")
    @DeleteMapping("/{seriesId}")
    public ApiResponse<Void> deleteSeries(
            @Parameter(description = "시리즈 ID") @PathVariable String seriesId,
            @CurrentUser AuthUser user
    ) {
        seriesService.deleteSeries(seriesId, user.uuid());
        return ApiResponse.success(null);
    }

    @Operation(summary = "시리즈 상세 조회")
    @GetMapping("/{seriesId}")
    public ApiResponse<SeriesResponse> getSeriesById(
            @Parameter(description = "시리즈 ID") @PathVariable String seriesId
    ) {
        SeriesResponse response = seriesService.getSeriesById(seriesId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "작성자별 시리즈 목록 조회")
    @GetMapping("/author/{authorId}")
    public ApiResponse<List<SeriesListResponse>> getSeriesByAuthor(
            @Parameter(description = "작성자 ID") @PathVariable String authorId
    ) {
        List<SeriesListResponse> responses = seriesService.getSeriesByAuthor(authorId);
        return ApiResponse.success(responses);
    }

    @Operation(summary = "내 시리즈 목록 조회")
    @GetMapping("/my")
    public ApiResponse<List<SeriesListResponse>> getMySeries(
            @CurrentUser AuthUser user
    ) {
        List<SeriesListResponse> responses = seriesService.getSeriesByAuthor(user.uuid());
        return ApiResponse.success(responses);
    }

    @Operation(summary = "시리즈에 포함된 포스트 목록 조회")
    @GetMapping("/{seriesId}/posts")
    public ApiResponse<List<PostSummaryResponse>> getSeriesPosts(
            @Parameter(description = "시리즈 ID") @PathVariable String seriesId
    ) {
        List<PostSummaryResponse> responses = seriesService.getSeriesPosts(seriesId);
        return ApiResponse.success(responses);
    }

    @Operation(summary = "시리즈에 포스트 추가")
    @PostMapping("/{seriesId}/posts/{postId}")
    public ApiResponse<SeriesResponse> addPostToSeries(
            @Parameter(description = "시리즈 ID") @PathVariable String seriesId,
            @Parameter(description = "포스트 ID") @PathVariable String postId,
            @CurrentUser AuthUser user
    ) {
        SeriesResponse response = seriesService.addPostToSeries(seriesId, postId, user.uuid());
        return ApiResponse.success(response);
    }

    @Operation(summary = "시리즈에서 포스트 제거")
    @DeleteMapping("/{seriesId}/posts/{postId}")
    public ApiResponse<SeriesResponse> removePostFromSeries(
            @Parameter(description = "시리즈 ID") @PathVariable String seriesId,
            @Parameter(description = "포스트 ID") @PathVariable String postId,
            @CurrentUser AuthUser user
    ) {
        SeriesResponse response = seriesService.removePostFromSeries(seriesId, postId, user.uuid());
        return ApiResponse.success(response);
    }

    @Operation(summary = "시리즈 내 포스트 순서 변경")
    @PutMapping("/{seriesId}/posts/order")
    public ApiResponse<SeriesResponse> reorderPosts(
            @Parameter(description = "시리즈 ID") @PathVariable String seriesId,
            @Valid @RequestBody SeriesPostOrderRequest request,
            @CurrentUser AuthUser user
    ) {
        SeriesResponse response = seriesService.reorderPosts(seriesId, request, user.uuid());
        return ApiResponse.success(response);
    }

    @Operation(summary = "특정 포스트가 포함된 시리즈 조회")
    @GetMapping("/by-post/{postId}")
    public ApiResponse<List<SeriesListResponse>> getSeriesByPostId(
            @Parameter(description = "포스트 ID") @PathVariable String postId
    ) {
        List<SeriesListResponse> responses = seriesService.getSeriesByPostId(postId);
        return ApiResponse.success(responses);
    }
}
