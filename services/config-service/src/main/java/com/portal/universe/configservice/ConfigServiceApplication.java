package com.portal.universe.configservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * Spring Cloud Config 서비스의 메인 애플리케이션 클래스입니다.
 */
@SpringBootApplication
@EnableConfigServer // 이 어노테이션을 통해 Spring Cloud Config Server로 동작하도록 설정합니다.
public class ConfigServiceApplication {

    /**
     * 애플리케이션을 시작하는 메인 메서드입니다.
     * @param args 커맨드 라인 인자
     */
    public static void main(String[] args) {
        SpringApplication.run(ConfigServiceApplication.class, args);
    }

}