package com.portal.universe.blogservice.tag.controller;

import com.portal.universe.blogservice.tag.dto.*;
import com.portal.universe.blogservice.tag.service.TagService;
import com.portal.universe.commonlibrary.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@Tag(name = "Tag", description = "태그 관리 및 검색 API")
@RestController
@RequestMapping("/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @Operation(summary = "태그 생성")
    @PostMapping
    public ApiResponse<TagResponse> createTag(@Valid @RequestBody TagCreateRequest request) {
        TagResponse response = tagService.createTag(request);
        return ApiResponse.success(response);
    }

    @Operation(summary = "전체 태그 목록 조회")
    @GetMapping
    public ApiResponse<List<TagResponse>> getAllTags() {
        List<TagResponse> responses = tagService.getAllTags();
        return ApiResponse.success(responses);
    }

    @Operation(summary = "태그 상세 조회")
    @GetMapping("/{tagName}")
    public ApiResponse<TagResponse> getTagByName(
            @Parameter(description = "태그 이름") @PathVariable String tagName
    ) {
        TagResponse response = tagService.getTagByName(tagName);
        return ApiResponse.success(response);
    }

    @Operation(summary = "인기 태그 조회")
    @GetMapping("/popular")
    public ApiResponse<List<TagStatsResponse>> getPopularTags(
            @Parameter(description = "조회 개수") @RequestParam(defaultValue = "10") int limit
    ) {
        List<TagStatsResponse> responses = tagService.getPopularTags(limit);
        return ApiResponse.success(responses);
    }

    @Operation(summary = "최근 사용된 태그 조회")
    @GetMapping("/recent")
    public ApiResponse<List<TagResponse>> getRecentlyUsedTags(
            @Parameter(description = "조회 개수") @RequestParam(defaultValue = "10") int limit
    ) {
        List<TagResponse> responses = tagService.getRecentlyUsedTags(limit);
        return ApiResponse.success(responses);
    }

    @Operation(summary = "태그 검색 (자동완성)")
    @GetMapping("/search")
    public ApiResponse<List<TagResponse>> searchTags(
            @Parameter(description = "검색 키워드") @RequestParam String q,
            @Parameter(description = "조회 개수") @RequestParam(defaultValue = "5") int limit
    ) {
        List<TagResponse> responses = tagService.searchTags(q, limit);
        return ApiResponse.success(responses);
    }

    @Operation(summary = "태그 설명 업데이트")
    @PatchMapping("/{tagName}/description")
    public ApiResponse<TagResponse> updateTagDescription(
            @Parameter(description = "태그 이름") @PathVariable String tagName,
            @Parameter(description = "태그 설명") @RequestParam String description
    ) {
        TagResponse response = tagService.updateTagDescription(tagName, description);
        return ApiResponse.success(response);
    }

    @Operation(summary = "사용되지 않는 태그 일괄 삭제 (관리자)")
    @DeleteMapping("/unused")
    @PreAuthorize("hasAnyAuthority('ROLE_BLOG_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ApiResponse<Void> deleteUnusedTags() {
        tagService.deleteUnusedTags();
        return ApiResponse.success(null);
    }

    @Operation(summary = "태그 강제 삭제 (관리자)")
    @DeleteMapping("/{tagName}")
    @PreAuthorize("hasAnyAuthority('ROLE_BLOG_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ApiResponse<Void> deleteTag(
            @Parameter(description = "태그 이름") @PathVariable String tagName
    ) {
        tagService.deleteTag(tagName);
        return ApiResponse.success(null);
    }
}