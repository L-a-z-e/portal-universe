package com.portal.universe.authservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Spring Authorization Server의 핵심 설정을 구성하는 클래스입니다.
 * OAuth2 클라이언트 정보, 토큰 커스터마이징, 서버 발급자(issuer) 정보 등을 정의합니다.
 */
@Configuration
public class AuthorizationServerConfig {

    @Value("${oauth2.client.redirect-uris:http://localhost:30000/callback}")
    private String[] redirectUris;

    @Value("${oauth2.client.post-logout-redirect-uris:http://localhost:30000}")
    private String[] postLogoutRedirectUris;

    /**
     * OAuth2 클라이언트의 정보를 등록하고 관리하는 저장소 Bean을 생성합니다.
     *
     * @return RegisteredClientRepository 클라이언트 정보 저장소
     */
    @Bean
    public RegisteredClientRepository registeredClientRepository() {
        String clientId = "portal-client";

        RegisteredClient.Builder builder = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(clientId)
                // Public Client이므로 Client Secret을 사용하지 않음 (PKCE 사용)
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                // 지원할 인가 방식 설정
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                // 클라이언트가 요청할 수 있는 스코프 정의
                .scope("read")
                .scope("write")
                .scope("openid")
                .scope("profile")
                .clientSettings(ClientSettings.builder()
                        .requireProofKey(true) // PKCE(Proof Key for Code Exchange) 강제
                        .requireAuthorizationConsent(false) // 사용자 동의 화면 생략
                        .build()
                )
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofMinutes(2))
                        .refreshTokenTimeToLive(Duration.ofDays(7))
                        .reuseRefreshTokens(false)
                        .idTokenSignatureAlgorithm(SignatureAlgorithm.RS256)
                        .build()
                );

        // 설정 파일에서 읽어온 Redirect URIs 추가
        for (String uri : redirectUris) {
            builder.redirectUri(uri.trim());
        }

        // 설정 파일에서 읽어온 Post Logout Redirect URIs 추가
        for (String uri : postLogoutRedirectUris) {
            builder.postLogoutRedirectUri(uri.trim());
        }

        return new InMemoryRegisteredClientRepository(builder.build());
    }

    /**
     * JWT Access Token에 추가적인 정보를 담기 위한 커스터마이저 Bean을 생성합니다.
     * 예를 들어, 사용자의 권한(roles) 정보를 토큰 내에 포함시켜 리소스 서버에서 활용할 수 있습니다.
     *
     * @return OAuth2TokenCustomizer JWT 커스터마이저
     */
    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer() {
        return context -> {
            // Access Token을 생성할 때만 동작하도록 필터링합니다.
            if (context.getTokenType().equals(OAuth2TokenType.ACCESS_TOKEN)) {
                Authentication principal = context.getPrincipal();
                // 사용자의 권한(authorities) 정보를 Set<String> 형태로 변환합니다.
                Set<String> authorities = principal.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toSet());

                // 토큰의 'claims'에 'roles'라는 이름으로 권한 정보를 추가합니다.
                context.getClaims().claim("roles", authorities);
            }
        };
    }

    /**
     * 인증 서버의 발급자(Issuer) URI를 설정합니다.
     * JWT의 'iss' 클레임 값으로 사용되며, 토큰을 발급한 주체를 식별하는 데 사용애됩니다.
     *
     * @param issuerUri 외부 설정(application.yml)에서 주입된 issuer URI
     * @return AuthorizationServerSettings 인증 서버 설정 객체
     */
    @Bean
    public AuthorizationServerSettings authorizationServerSettings(@Value("${spring.security.oauth2.authorizationserver.issuer}") String issuerUri) {
        return AuthorizationServerSettings.builder()
                .issuer(issuerUri)
                .build();
    }
}