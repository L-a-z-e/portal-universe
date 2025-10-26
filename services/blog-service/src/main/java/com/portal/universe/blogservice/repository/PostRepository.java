package com.portal.universe.blogservice.repository;

import com.portal.universe.blogservice.domain.Post;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * Post 도큐먼트에 대한 데이터 접근을 처리하는 Spring Data MongoDB 리포지토리입니다.
 */
public interface PostRepository extends MongoRepository<Post,String> {
    /**
     * 주어진 상품 ID(productId)에 해당하는 모든 게시물 목록을 조회합니다.
     * @param productId 조회할 상품의 ID
     * @return 해당 상품 ID를 가진 게시물 목록
     */
    List<Post> findByProductId(String productId);
}