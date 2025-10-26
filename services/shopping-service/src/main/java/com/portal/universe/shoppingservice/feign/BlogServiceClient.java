package com.portal.universe.shoppingservice.feign;

import com.portal.universe.shoppingservice.dto.BlogResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Blog 서비스와 통신하기 위한 OpenFeign 클라이언트 인터페이스입니다.
 * 이 인터페이스를 통해 Blog 서비스의 API를 마치 로컬 메서드처럼 호출할 수 있습니다.
 *
 * name: 호출할 마이크로서비스의 Eureka 등록 이름
 * path: 이 클라이언트의 모든 요청 URL에 공통으로 적용될 접두사 경로
 */
@FeignClient(name = "blog-service", path = "/api/blog")
public interface BlogServiceClient {

    /**
     * Blog 서비스의 'GET /api/blog/reviews' 엔드포인트를 호출하여
     * 특정 상품 ID에 해당하는 리뷰 목록을 가져옵니다.
     *
     * @param productId 조회할 상품의 ID
     * @return 해당 상품에 대한 리뷰(게시물) 목록
     */
    @GetMapping("/reviews")
    List<BlogResponse> getPostByProductId(@RequestParam("productId") String productId);
}