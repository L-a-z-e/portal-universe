package com.portal.universe.authservice.follow.controller;

import com.portal.universe.authservice.follow.dto.FollowingIdsResponse;
import com.portal.universe.authservice.follow.service.FollowService;
import com.portal.universe.commonlibrary.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/follow")
@RequiredArgsConstructor
public class FollowInternalController {

    private final FollowService followService;

    @GetMapping("/{userUuid}/following-ids")
    public ApiResponse<FollowingIdsResponse> getFollowingIds(
            @PathVariable String userUuid
    ) {
        return ApiResponse.success(followService.getMyFollowingIdsByUuid(userUuid));
    }
}
