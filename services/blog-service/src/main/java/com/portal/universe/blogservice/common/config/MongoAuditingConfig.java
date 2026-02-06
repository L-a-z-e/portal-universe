package com.portal.universe.blogservice.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * MongoDB Auditing 설정 분리.
 * @WebMvcTest 에서 MongoDB 관련 bean 로드 실패를 방지하기 위해
 * @EnableMongoAuditing 을 별도 @Configuration 클래스로 분리합니다.
 */
@Configuration
@EnableMongoAuditing
public class MongoAuditingConfig {
}
