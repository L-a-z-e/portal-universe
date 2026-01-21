package com.portal.universe.blogservice.like.controller;

import com.portal.universe.blogservice.like.dto.LikeStatusResponse;
import com.portal.universe.blogservice.like.dto.LikeToggleResponse;
import com.portal.universe.blogservice.like.dto.LikerResponse;
import com.portal.universe.blogservice.like.service.LikeService;
import com.portal.universe.commonlibrary.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 좋아요 관리 REST API Controller
 * 포스트 좋아요 추가/취소, 상태 조회, 좋아요한 사용자 목록 조회
 */
@Tag(name = "Like", description = "좋아요 관리 API")
@RestController
@RequestMapping("/posts/{postId}")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;

    @Operation(
            summary = "좋아요 토글",
            description = "포스트에 좋아요를 추가하거나 취소합니다. 이미 좋아요가 있으면 취소, 없으면 추가됩니다."
    )
    @PostMapping("/like")
    public ApiResponse<LikeToggleResponse> toggleLike(
            @Parameter(description = "포스트 ID") @PathVariable String postId,
            @AuthenticationPrincipal String userId,
            @RequestHeader(value = "X-User-Name", required = false) String userName
    ) {
        LikeToggleResponse response = likeService.toggleLike(postId, userId, userName);
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "좋아요 상태 확인",
            description = "현재 사용자의 좋아요 여부와 전체 좋아요 수를 조회합니다."
    )
    @GetMapping("/like")
    public ApiResponse<LikeStatusResponse> getLikeStatus(
            @Parameter(description = "포스트 ID") @PathVariable String postId,
            @AuthenticationPrincipal String userId
    ) {
        LikeStatusResponse response = likeService.getLikeStatus(postId, userId);
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "좋아요한 사용자 목록 조회",
            description = "해당 포스트를 좋아요한 사용자 목록을 페이징하여 조회합니다. 최신순으로 정렬됩니다."
    )
    @GetMapping("/likes")
    public ApiResponse<Page<LikerResponse>> getLikers(
            @Parameter(description = "포스트 ID") @PathVariable String postId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        Page<LikerResponse> response = likeService.getLikers(postId, pageable);
        return ApiResponse.success(response);
    }
}
