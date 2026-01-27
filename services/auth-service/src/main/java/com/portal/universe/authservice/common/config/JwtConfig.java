package com.portal.universe.authservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * application.yml의 jwt 설정을 바인딩하는 클래스입니다.
 * JWT 토큰 생성 시 필요한 비밀키와 만료 시간을 관리합니다.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {

    /**
     * JWT 서명에 사용되는 비밀키
     * 최소 256비트(32바이트) 이상이어야 합니다.
     */
    private String secretKey;

    /**
     * Access Token 만료 시간 (밀리초)
     * 기본값: 900000ms (15분)
     */
    private long accessTokenExpiration;

    /**
     * Refresh Token 만료 시간 (밀리초)
     * 기본값: 604800000ms (7일)
     */
    private long refreshTokenExpiration;
}
