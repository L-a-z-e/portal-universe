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
 * **Reactive(Spring WebFlux) 환경**에서 JWT의 `roles` 클레임을 Spring Security의 `GrantedAuthority` 객체로 변환하는 역할을 하는 컨버터입니다.
 * {@link Converter} 인터페이스를 구현하여 JWT를 {@code Mono<AbstractAuthenticationToken>}으로 변환합니다.
 * <p>
 * 주로 API Gateway와 같은 WebFlux 기반 서비스에서 사용됩니다.
 */
public class ReactiveJwtAuthenticationConverterAdapter
        implements Converter<Jwt, Mono<AbstractAuthenticationToken>> {

    private final String authoritiesClaimName;
    private final String authorityPrefix;

    /**
     * 커스텀 설정을 사용하여 컨버터를 생성합니다.
     *
     * @param authoritiesClaimName JWT에서 권한 목록을 담고 있는 클레임의 이름
     * @param authorityPrefix 각 권한 문자열 앞에 추가할 접두사
     */
    public ReactiveJwtAuthenticationConverterAdapter(
            String authoritiesClaimName,
            String authorityPrefix
    ) {
        this.authoritiesClaimName = authoritiesClaimName;
        this.authorityPrefix = authorityPrefix;
    }

    /**
     * 기본 설정을 사용하는 생성자입니다.
     * - **클레임 이름**: "roles"
     * - **권한 접두사**: "" (없음). Auth-Service에서 이미 "ROLE_USER"와 같이 완전한 형태로 권한을 보내주기 때문입니다.
     */
    public ReactiveJwtAuthenticationConverterAdapter() {
        this("roles", "");
    }

    /**
     * JWT를 인증 토큰(Authentication Token)으로 변환하는 핵심 로직입니다.
     * @param jwt Spring Security가 디코딩한 JWT 객체
     * @return 사용자의 권한 정보가 포함된 {@link JwtAuthenticationToken}을 담은 Mono
     */
    @Override
    public Mono<AbstractAuthenticationToken> convert(Jwt jwt) {
        // 1. JWT에서 "roles" 클레임을 문자열 컬렉션으로 추출합니다.
        Collection<String> authorities = jwt.getClaimAsStringList(authoritiesClaimName);

        if (authorities == null) {
            authorities = Collections.emptyList();
        }

        // 2. 추출된 권한 문자열들을 `GrantedAuthority` 객체 컬렉션으로 변환합니다.
        Collection<GrantedAuthority> grantedAuthorities = authorities.stream()
                .map(authority -> new SimpleGrantedAuthority(authorityPrefix + authority))
                .collect(Collectors.toList());

        // 3. JWT와 변환된 권한 정보를 사용하여 `JwtAuthenticationToken`을 생성하고 Mono로 감싸 반환합니다.
        return Mono.just(new JwtAuthenticationToken(jwt, grantedAuthorities));
    }
}
