package com.portal.universe.discoveryservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Spring Cloud Eureka 서비스 레지스트리의 메인 애플리케이션 클래스입니다.
 */
@SpringBootApplication
@EnableEurekaServer // 이 어노테이션을 통해 이 애플리케이션을 Eureka 서버로 활성화합니다.
public class DiscoveryServiceApplication {

	/**
	 * 애플리케이션을 시작하는 메인 메서드입니다.
	 * @param args 커맨드 라인 인자
	 */
	public static void main(String[] args) {
		SpringApplication.run(DiscoveryServiceApplication.class, args);
	}

}
