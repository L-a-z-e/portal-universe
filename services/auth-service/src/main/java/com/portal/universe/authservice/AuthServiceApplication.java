package com.portal.universe.authservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 인증(Auth) 서비스의 메인 애플리케이션 클래스입니다.
 * Spring Boot 애플리케이션의 진입점(Entry Point) 역할을 합니다.
 */
// 공통 라이브러리(common-library)에 정의된 Bean들을 스캔하기 위해 패키지 범위를 명시적으로 지정합니다.
// 주의: 별도의 @ComponentScan 대신 scanBasePackages를 사용해야 @WebMvcTest의 TypeExcludeFilter가 정상 동작합니다.
// 주의: @EnableJpaAuditing은 JpaAuditingConfig로 분리해야 @WebMvcTest에서 JPA metamodel 에러가 발생하지 않습니다.
@SpringBootApplication(scanBasePackages = { "com.portal.universe.authservice", "com.portal.universe.commonlibrary" })
public class AuthServiceApplication {

    /**
     * 애플리케이션을 시작하는 메인 메서드입니다.
     * @param args 커맨드 라인 인자
     */
    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }

}