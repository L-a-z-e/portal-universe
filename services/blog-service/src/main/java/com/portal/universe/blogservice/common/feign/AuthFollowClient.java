package com.portal.universe.blogservice.common.feign;

import com.portal.universe.commonlibrary.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "auth-service", url = "${feign.auth-service.url}", path = "/api/v1/internal/follow")
public interface AuthFollowClient {

    @GetMapping("/{userUuid}/following-ids")
    ApiResponse<FollowingIdsDto> getFollowingIds(@PathVariable String userUuid);

    record FollowingIdsDto(List<String> followingIds) {}
}
