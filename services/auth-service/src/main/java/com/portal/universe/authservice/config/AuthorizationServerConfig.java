package com.portal.universe.authservice.config;

import org.springframework.context.annotation.Configuration;

/**
 * JWT 인증 설정을 담당하는 클래스입니다.
 *
 * Direct JWT 인증 방식으로 전환되어, Spring Authorization Server(OIDC)는 사용하지 않습니다.
 * JWT 토큰은 대칭키(HMAC-SHA256)로 서명되며, TokenService에서 생성/검증됩니다.
 *
 * 제거된 항목:
 * - JWKS 엔드포인트: 대칭키 방식에서는 불필요
 * - OAuth2 Client Repository: Direct JWT 방식에서는 불필요
 * - OAuth2 Token Customizer: TokenService에서 직접 claims 설정
 */
@Configuration
public class AuthorizationServerConfig {
    // Direct JWT 방식에서는 추가 설정이 필요 없습니다.
    // JWT 생성/검증은 TokenService에서 담당합니다.
}
