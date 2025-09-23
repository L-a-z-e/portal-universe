package com.portal.universe.shoppingservice.feign;

import com.portal.universe.shoppingservice.dto.BlogResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

// name: 호출할 서비스의 Eureka 등록 이름
// path: 이 클라이언트의 모든 요청에 공통으로 적용될 접두 경로
@FeignClient(name = "blog-service", path = "/api/blog")
public interface BlogServiceClient {

    @GetMapping("/reviews")
    List<BlogResponse> getPostByProductId(@RequestParam("productId") String productId);
}
