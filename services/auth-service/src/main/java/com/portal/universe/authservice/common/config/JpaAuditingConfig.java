package com.portal.universe.authservice.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing 설정 클래스입니다.
 * @EnableJpaAuditing을 별도 @Configuration 클래스에 분리하여
 * @WebMvcTest에서 JPA metamodel 에러를 방지합니다.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
