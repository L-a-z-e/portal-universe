package com.portal.universe.blogservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.TextIndexDefinition;

@Configuration
@RequiredArgsConstructor
public class MongoConfig implements InitializingBean {

    private final MongoTemplate mongoTemplate;

    @Override
    public void afterPropertiesSet() throws Exception {
        createIndexes();
    }

    /**
     * PRD Phase 1: 블로그 최적화를 위한 인덱스 생성
     */
    private void createIndexes() {
        IndexOperations indexOps = mongoTemplate.indexOps("posts");

        // 1. 전문 검색 인덱스 (제목 가중치 2.0, 내용 가중치 1.0)
        TextIndexDefinition textIndex = TextIndexDefinition.builder()
                .onField("title", 2.0f)
                .onField("content", 1.0f)
                .build();
        indexOps.ensureIndex(textIndex);

        // 2. 발행된 게시물 조회 최적화 (메인 페이지)
        indexOps.ensureIndex(
                Index.on("status", Sort.Direction.ASC)
                        .on("publishedAt", Sort.Direction.DESC)
        );

        // 3. 작성자별 조회 최적화 (마이페이지)
        indexOps.ensureIndex(
                Index.on("authorId", Sort.Direction.ASC)
                        .on("createdAt", Sort.Direction.DESC)
        );

        // 4. 카테고리별 조회 최적화 (카테고리 페이지)
        indexOps.ensureIndex(
                Index.on("category", Sort.Direction.ASC)
                        .on("status", Sort.Direction.ASC)
                        .on("publishedAt", Sort.Direction.DESC)
        );

        // 5. 태그 검색 최적화
        indexOps.ensureIndex(Index.on("tags", Sort.Direction.ASC));

        // 6. 인기 게시물 조회 최적화 (조회수 순)
        indexOps.ensureIndex(
                Index.on("status", Sort.Direction.ASC)
                        .on("viewCount", Sort.Direction.DESC)
                        .on("publishedAt", Sort.Direction.DESC)
        );

        // 7. 기존 호환성: 상품별 조회 최적화
        indexOps.ensureIndex(Index.on("productId", Sort.Direction.ASC));

        System.out.println("✅ MongoDB 인덱스 생성 완료");
    }
}