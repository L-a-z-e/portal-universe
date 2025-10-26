package com.portal.universe.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway 서비스의 메인 애플리케이션 클래스입니다.
 * Spring Boot 애플리케이션의 진입점(Entry Point) 역할을 합니다.
 */
@SpringBootApplication
public class ApiGatewayApplication {

    /**
     * 애플리케이션을 시작하는 메인 메서드입니다.
     * @param args 커맨드 라인 인자
     */
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }

}