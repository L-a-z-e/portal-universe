package com.portal.universe.authservice.follow.controller;

import com.portal.universe.authservice.follow.dto.*;
import com.portal.universe.authservice.follow.service.FollowService;
import com.portal.universe.commonlibrary.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 팔로우 관련 API를 처리하는 컨트롤러입니다.
 */
@Tag(name = "Follow", description = "팔로우 관리 API")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    @Operation(summary = "팔로우 토글", description = "이미 팔로우 중이면 언팔로우, 아니면 팔로우합니다.")
    @PostMapping("/{username}/follow")
    public ResponseEntity<ApiResponse<FollowResponse>> toggleFollow(
            @Parameter(description = "대상 사용자 username") @PathVariable String username,
            @AuthenticationPrincipal String userUuid
    ) {
        FollowResponse response = followService.toggleFollowByUuid(userUuid, username);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "팔로워 목록 조회", description = "특정 사용자의 팔로워 목록을 조회합니다.")
    @GetMapping("/{username}/followers")
    public ResponseEntity<ApiResponse<FollowListResponse>> getFollowers(
            @Parameter(description = "사용자 username") @PathVariable String username,
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size
    ) {
        FollowListResponse response = followService.getFollowers(username, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "팔로잉 목록 조회", description = "특정 사용자가 팔로우하는 목록을 조회합니다.")
    @GetMapping("/{username}/following")
    public ResponseEntity<ApiResponse<FollowListResponse>> getFollowings(
            @Parameter(description = "사용자 username") @PathVariable String username,
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size
    ) {
        FollowListResponse response = followService.getFollowings(username, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "내 팔로잉 ID 목록 조회", description = "피드 API에서 사용하기 위한 팔로잉 사용자 UUID 목록을 조회합니다.")
    @GetMapping("/me/following/ids")
    public ResponseEntity<ApiResponse<FollowingIdsResponse>> getMyFollowingIds(
            @AuthenticationPrincipal String userUuid
    ) {
        FollowingIdsResponse response = followService.getMyFollowingIdsByUuid(userUuid);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "팔로우 상태 확인", description = "현재 로그인한 사용자가 특정 사용자를 팔로우하는지 확인합니다.")
    @GetMapping("/{username}/follow/status")
    public ResponseEntity<ApiResponse<FollowStatusResponse>> getFollowStatus(
            @Parameter(description = "대상 사용자 username") @PathVariable String username,
            @AuthenticationPrincipal String userUuid
    ) {
        FollowStatusResponse response = followService.getFollowStatusByUuid(userUuid, username);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
