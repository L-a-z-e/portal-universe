package com.portal.universe.shoppingservice.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA 관련 설정을 담당하는 클래스입니다.
 * JPA Auditing을 활성화하여 엔티티의 생성/수정 시간을 자동으로 관리합니다.
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
