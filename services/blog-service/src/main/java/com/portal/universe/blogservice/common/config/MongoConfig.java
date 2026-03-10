package com.portal.universe.blogservice.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.TextIndexDefinition;

/**
 * MongoDB 트랜잭션 관리 및 인덱스 생성 설정.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class MongoConfig implements InitializingBean {

    private final MongoTemplate mongoTemplate;

    @Bean
    MongoTransactionManager transactionManager(MongoDatabaseFactory dbFactory) {
        return new MongoTransactionManager(dbFactory);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        createIndexes();
    }

    private void createIndexes() {
        IndexOperations indexOps = mongoTemplate.indexOps("posts");

        // 1. 전문 검색 인덱스 (제목 가중치 2.0, 내용 가중치 1.0)
        TextIndexDefinition textIndex = TextIndexDefinition.builder()
                .onField("title", 2.0f)
                .onField("content", 1.0f)
                .build();
        indexOps.createIndex(textIndex);

        // 2. 발행된 게시물 조회 최적화 (메인 페이지)
        indexOps.createIndex(
                new Index()
                        .on("status", Sort.Direction.ASC)
                        .on("publishedAt", Sort.Direction.DESC)
        );

        // 3. 작성자별 조회 최적화 (마이페이지)
        indexOps.createIndex(
                new Index()
                        .on("authorId", Sort.Direction.ASC)
                        .on("createdAt", Sort.Direction.DESC)
        );

        // 4. 카테고리별 조회 최적화 (카테고리 페이지)
        indexOps.createIndex(
                new Index()
                        .on("category", Sort.Direction.ASC)
                        .on("status", Sort.Direction.ASC)
                        .on("publishedAt", Sort.Direction.DESC)
        );

        // 5. 태그 검색 최적화
        indexOps.createIndex(
                new Index()
                        .on("tags", Sort.Direction.ASC)
        );

        // 6. 인기 게시물 조회 최적화 (조회수 순)
        indexOps.createIndex(
                new Index()
                        .on("status", Sort.Direction.ASC)
                        .on("viewCount", Sort.Direction.DESC)
                        .on("publishedAt", Sort.Direction.DESC)
        );

        // 7. 기존 호환성: 상품별 조회 최적화
        indexOps.createIndex(
                new Index()
                        .on("productId", Sort.Direction.ASC)
        );

        log.info("MongoDB posts indexes created");

        // Tags 컬렉션 인덱스
        createTagIndexes();
    }

    private void createTagIndexes() {
        IndexOperations tagOps = mongoTemplate.indexOps("tags");

        // 1. 인기 태그 조회: postCount DESC (ESR: S=R 동일 필드)
        tagOps.createIndex(
                new Index()
                        .on("postCount", Sort.Direction.DESC)
        );

        // 2. 최근 사용 태그 조회: lastUsedAt DESC → postCount (ESR: S→R)
        tagOps.createIndex(
                new Index()
                        .on("lastUsedAt", Sort.Direction.DESC)
                        .on("postCount", Sort.Direction.ASC)
        );

        log.info("MongoDB tags indexes created");
    }
}