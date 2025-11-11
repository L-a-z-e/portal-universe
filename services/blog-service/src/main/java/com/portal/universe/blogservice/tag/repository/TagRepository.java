package com.portal.universe.blogservice.tag.repository;

import com.portal.universe.blogservice.tag.domain.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

/**
 * 태그 Repository
 */
public interface TagRepository extends MongoRepository<Tag, String> {

    /**
     * 태그 이름으로 조회 (대소문자 구분 없음)
     */
    Optional<Tag> findByNameIgnoreCase(String name);

    /**
     * 태그 이름 존재 여부 확인
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * 인기 태그 조회 (postCount 기준 내림차순)
     */
    List<Tag> findByPostCountGreaterThanOrderByPostCountDesc(Long minCount, Pageable pageable);

    /**
     * 최근 사용된 태그 조회
     */
    List<Tag> findByPostCountGreaterThanOrderByLastUsedAtDesc(Long minCount, Pageable pageable);

    /**
     * 태그 이름 검색 (부분 일치, 대소문자 구분 없음)
     */
    List<Tag> findByNameContainingIgnoreCaseOrderByPostCountDesc(String keyword, Pageable pageable);

    /**
     * 사용되지 않는 태그 조회 (정리용)
     */
    List<Tag> findByPostCount(Long postCount);
}