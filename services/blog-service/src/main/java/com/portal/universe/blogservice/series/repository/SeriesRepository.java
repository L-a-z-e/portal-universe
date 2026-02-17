package com.portal.universe.blogservice.series.repository;

import com.portal.universe.blogservice.series.domain.Series;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * 시리즈 Repository
 */
public interface SeriesRepository extends MongoRepository<Series, String> {

    /**
     * 전체 시리즈 목록 조회 (최신순)
     */
    List<Series> findAllByOrderByUpdatedAtDesc();

    /**
     * 작성자별 시리즈 목록 조회 (최신순)
     */
    List<Series> findByAuthorIdOrderByCreatedAtDesc(String authorId);

    /**
     * 특정 포스트가 포함된 시리즈 조회
     */
    List<Series> findByPostIdsContaining(String postId);

    /**
     * 작성자 시리즈 개수
     */
    long countByAuthorId(String authorId);
}