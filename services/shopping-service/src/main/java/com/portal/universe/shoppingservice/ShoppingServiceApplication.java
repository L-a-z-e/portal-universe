package com.portal.universe.shoppingservice;

import com.portal.universe.shoppingservice.config.FeignClientConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

/**
 * 쇼핑(Shopping) 서비스의 메인 애플리케이션 클래스입니다.
 * Spring Boot 애플리케이션의 진입점(Entry Point) 역할을 합니다.
 */
@SpringBootApplication
// Feign 클라이언트 기능을 활성화하고, 모든 Feign 클라이언트에 대한 기본 설정을 지정합니다.
@EnableFeignClients(defaultConfiguration = FeignClientConfig.class)
// 공통 라이브러리(common-library)에 정의된 Bean들을 스캔하기 위해 패키지 범위를 명시적으로 지정합니다.
@ComponentScan(basePackages = { "com.portal.universe.shoppingservice", "com.portal.universe.commonlibrary" })
public class ShoppingServiceApplication {
    /**
     * 애플리케이션을 시작하는 메인 메서드입니다.
     * @param args 커맨드 라인 인자
     */
    public static void main(String[] args) {
        SpringApplication.run(ShoppingServiceApplication.class, args);
    }
}