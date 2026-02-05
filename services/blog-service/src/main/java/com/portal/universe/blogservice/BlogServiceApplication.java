package com.portal.universe.blogservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 블로그(Blog) 서비스의 메인 애플리케이션 클래스입니다.
 * Spring Boot 애플리케이션의 진입점(Entry Point) 역할을 합니다.
 *
 * @EnableMongoAuditing 은 MongoAuditingConfig 로 분리되어 있습니다.
 */
@SpringBootApplication(scanBasePackages = {
        "com.portal.universe.blogservice",
        "com.portal.universe.commonlibrary"
})
public class BlogServiceApplication {

    /**
     * 애플리케이션을 시작하는 메인 메서드입니다.
     * @param args 커맨드 라인 인자
     */
    public static void main(String[] args) {
        SpringApplication.run(BlogServiceApplication.class, args);
    }

}