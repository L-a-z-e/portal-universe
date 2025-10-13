package com.portal.universe.commonlibrary.security.converter;

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
 * Reactive(WebFlux) 기반 서비스에서 JWT의 roles 클레임을 GrantedAuthority로 변환
 *
 * 사용 서비스:
 * - api-gateway (모든 요청의 JWT 검증)
 */
public class ReactiveJwtAuthenticationConverterAdapter
        implements Converter<Jwt, Mono<AbstractAuthenticationToken>> {

    private final String authoritiesClaimName;
    private final String authorityPrefix;

    /**
     * 커스텀 설정으로 Converter 생성
     *
     * @param authoritiesClaimName JWT에서 권한 정보를 읽을 클레임 이름
     * @param authorityPrefix 권한 앞에 붙일 접두사
     */
    public ReactiveJwtAuthenticationConverterAdapter(
            String authoritiesClaimName,
            String authorityPrefix
    ) {
        this.authoritiesClaimName = authoritiesClaimName;
        this.authorityPrefix = authorityPrefix;
    }

    /**
     * 기본 생성자
     * - 클레임 이름: "roles"
     * - 접두사: "" (Auth Service가 이미 "ROLE_ADMIN" 형태로 보냄)
     */
    public ReactiveJwtAuthenticationConverterAdapter() {
        this("roles", "");
    }

    @Override
    public Mono<AbstractAuthenticationToken> convert(Jwt jwt) {
        // JWT에서 "roles" 클레임 추출
        Collection<String> authorities = jwt.getClaimAsStringList(authoritiesClaimName);

        if (authorities == null) {
            authorities = Collections.emptyList();
        }

        // GrantedAuthority로 변환 (접두사 추가)
        Collection<GrantedAuthority> grantedAuthorities = authorities.stream()
                .map(authority -> new SimpleGrantedAuthority(authorityPrefix + authority))
                .collect(Collectors.toList());

        // JwtAuthenticationToken 생성 (Reactive 타입으로 반환)
        return Mono.just(new JwtAuthenticationToken(jwt, grantedAuthorities));
    }
}