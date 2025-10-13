package com.portal.universe.apigateway.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * JWT의 "roles" 클레임을 GrantedAuthority로 변환하는 WebFlux용 컨버터
 */
@Configuration
public class CustomJwtAuthenticationConverter implements Converter<Jwt, Mono<AbstractAuthenticationToken>> {

    @Override
    public Mono<AbstractAuthenticationToken> convert(Jwt jwt) {
        // 1. JWT에서 "roles" 클레임 추출
        Collection<String> roles = jwt.getClaimAsStringList("roles");

        if (roles == null) {
            roles = Collections.emptyList();
        }

        // 2. GrantedAuthority로 변환
        // Auth Service에서 이미 "ROLE_ADMIN" 형태로 보내므로 그대로 사용
        Collection<GrantedAuthority> authorities = roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        // 3. JwtAuthenticationToken 생성 (Reactive 타입으로 반환)
        return Mono.just(new JwtAuthenticationToken(jwt, authorities));
    }
}