package com.portal.universe.apigateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * application.yml의 jwt 설정을 바인딩하는 클래스입니다.
 * Auth Service와 동일한 secret key를 사용하여 JWT를 검증합니다.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {

    /**
     * JWT 서명 검증에 사용되는 비밀키
     * Auth Service와 동일한 키를 사용해야 합니다.
     */
    private String secretKey;
}
