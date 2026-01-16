package com.portal.universe.authservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * 인증(Auth) 서비스의 메인 애플리케이션 클래스입니다.
 * Spring Boot 애플리케이션의 진입점(Entry Point) 역할을 합니다.
 */
@EnableJpaAuditing
@SpringBootApplication
// 공통 라이브러리(common-library)에 정의된 Bean들을 스캔하기 위해 패키지 범위를 명시적으로 지정합니다.
@ComponentScan(basePackages = { "com.portal.universe.authservice", "com.portal.universe.commonlibrary" })
public class AuthServiceApplication {

    /**
     * 애플리케이션을 시작하는 메인 메서드입니다.
     * @param args 커맨드 라인 인자
     */
    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }

}