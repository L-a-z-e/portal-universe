package com.portal.universe.authservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Configuration
public class AuthorizationServerConfig {

    @Bean
    public RegisteredClientRepository registeredClientRepository(PasswordEncoder passwordEncoder) {
        // 실제 운영환경에서는 외부 설정 파일로 분리
        String clientId = "portal-client";
        String clientSecret = "secret";

        RegisteredClient registeredClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(clientId)
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .authorizationGrantType(AuthorizationGrantType.PASSWORD)
                .redirectUri("http://localhost:50000/callback")
                .postLogoutRedirectUri("http://localhost:50000")
                .scope("read")
                .scope("write")
                .scope("openid")
                .scope("profile")
                .clientSettings(ClientSettings.builder()
                                .requireProofKey(true)
                                .requireAuthorizationConsent(false)
                                .build()
                )
                .build();

        return new InMemoryRegisteredClientRepository(registeredClient);
    }

    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer() {
        return context -> {
            // Access Token을 생성할 때만 동작하도록 필터링
            if (context.getTokenType().equals(OAuth2TokenType.ACCESS_TOKEN)) {
                Authentication principal = context.getPrincipal();
                // 사용자의 권한(authorities)과 요청된 스코프(scopes)를 가져옵니다.
                Set<String> authorities = principal.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toSet());
                Set<String> scopes = context.getRegisteredClient().getScopes();

                // 'scope' 클레임에 권한과 스코프를 모두 포함시킵니다.
                context.getClaims().claim("scope", scopes);
                context.getClaims().claim("roles", authorities);
                // 다른 커스텀 클레임 등록 시
                // context.getClaims().claim("username", principal.getName());
            }
        };
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings(@Value("${spring.security.oauth2.authorizationserver.issuer}") String issuerUri) {
        return AuthorizationServerSettings.builder()
                .issuer(issuerUri)
                .build();
    }
}
