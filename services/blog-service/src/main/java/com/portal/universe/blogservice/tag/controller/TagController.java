package com.portal.universe.blogservice.tag.controller;

import com.portal.universe.blogservice.tag.dto.*;
import com.portal.universe.blogservice.tag.service.TagService;
import com.portal.universe.commonlibrary.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 태그 API Controller
 */
@RestController
@RequestMapping("/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    /**
     * 태그 생성
     * POST /api/tags
     */
    @PostMapping
    public ApiResponse<TagResponse> createTag(@Valid @RequestBody TagCreateRequest request) {
        TagResponse response = tagService.createTag(request);
        return ApiResponse.success(response);
    }

    /**
     * 전체 태그 목록 조회
     * GET /api/tags
     */
    @GetMapping
    public ApiResponse<List<TagResponse>> getAllTags() {
        List<TagResponse> responses = tagService.getAllTags();
        return ApiResponse.success(responses);
    }

    /**
     * 태그 상세 조회
     * GET /api/tags/{tagName}
     */
    @GetMapping("/{tagName}")
    public ApiResponse<TagResponse> getTagByName(@PathVariable String tagName) {
        TagResponse response = tagService.getTagByName(tagName);
        return ApiResponse.success(response);
    }

    /**
     * 인기 태그 조회
     * GET /api/tags/popular?limit=10
     */
    @GetMapping("/popular")
    public ApiResponse<List<TagStatsResponse>> getPopularTags(
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<TagStatsResponse> responses = tagService.getPopularTags(limit);
        return ApiResponse.success(responses);
    }

    /**
     * 최근 사용된 태그 조회
     * GET /api/tags/recent?limit=10
     */
    @GetMapping("/recent")
    public ApiResponse<List<TagResponse>> getRecentlyUsedTags(
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<TagResponse> responses = tagService.getRecentlyUsedTags(limit);
        return ApiResponse.success(responses);
    }

    /**
     * 태그 검색 (자동완성)
     * GET /api/tags/search?q=vue&limit=5
     */
    @GetMapping("/search")
    public ApiResponse<List<TagResponse>> searchTags(
            @RequestParam String q,
            @RequestParam(defaultValue = "5") int limit
    ) {
        List<TagResponse> responses = tagService.searchTags(q, limit);
        return ApiResponse.success(responses);
    }

    /**
     * 태그 설명 업데이트
     * PATCH /api/tags/{tagName}/description
     */
    @PatchMapping("/{tagName}/description")
    public ApiResponse<TagResponse> updateTagDescription(
            @PathVariable String tagName,
            @RequestParam String description
    ) {
        TagResponse response = tagService.updateTagDescription(tagName, description);
        return ApiResponse.success(response);
    }

    /**
     * 사용되지 않는 태그 삭제 (관리자용)
     * DELETE /api/tags/unused
     */
    @DeleteMapping("/unused")
    public ApiResponse<Void> deleteUnusedTags() {
        tagService.deleteUnusedTags();
        return ApiResponse.success(null);
    }

    /**
     * 태그 강제 삭제
     * DELETE /api/tags/{tagName}
     */
    @DeleteMapping("/{tagName}")
    public ApiResponse<Void> deleteTag(@PathVariable String tagName) {
        tagService.deleteTag(tagName);
        return ApiResponse.success(null);
    }
}