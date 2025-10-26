package com.portal.universe.commonlibrary.security.converter;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

/**
 * **Servlet(Spring MVC) 환경**에서 JWT의 `roles` 클레임을 Spring Security의 `GrantedAuthority` 객체로 변환하는 역할을 하는 어댑터 클래스입니다.
 * 정적 팩토리 메서드를 통해 필요한 {@link JwtAuthenticationConverter}를 생성합니다.
 */
public class JwtAuthenticationConverterAdapter {

    /**
     * JWT에서 권한 정보를 추출하는 {@link JwtAuthenticationConverter}를 생성합니다.
     *
     * @param authoritiesClaimName JWT에서 권한 목록을 담고 있는 클레임의 이름 (예: "roles", "scope")
     * @param authorityPrefix 각 권한 문자열 앞에 추가할 접두사 (예: "ROLE_")
     * @return 커스텀 설정이 적용된 JwtAuthenticationConverter 인스턴스
     */
    public static JwtAuthenticationConverter create(
            String authoritiesClaimName,
            String authorityPrefix
    ) {
        // 1. JWT의 특정 클레임에서 권한 문자열을 추출하는 컨버터를 생성합니다.
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter =
                new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthoritiesClaimName(authoritiesClaimName);
        grantedAuthoritiesConverter.setAuthorityPrefix(authorityPrefix);

        // 2. 위에서 생성한 컨버터를 사용하여 최종 JwtAuthenticationConverter를 설정합니다.
        JwtAuthenticationConverter jwtAuthenticationConverter =
                new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(
                grantedAuthoritiesConverter
        );

        return jwtAuthenticationConverter;
    }

    /**
     * 기본 설정을 사용하여 {@link JwtAuthenticationConverter}를 생성합니다.
     * - **클레임 이름**: "roles"
     * - **권한 접두사**: "" (없음). Auth-Service에서 이미 "ROLE_USER"와 같이 완전한 형태로 권한을 보내주기 때문입니다.
     *
     * @return 기본 설정이 적용된 JwtAuthenticationConverter 인스턴스
     */
    public static JwtAuthenticationConverter createDefault() {
        return create("roles", "");
    }
}
