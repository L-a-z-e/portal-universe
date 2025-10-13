package com.portal.universe.commonlibrary.security.converter;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

/**
 * Servlet(MVC) 기반 서비스에서 JWT의 roles 클레임을 GrantedAuthority로 변환
 */
public class JwtAuthenticationConverterAdapter {

    /**
     * JWT에서 권한 정보를 추출하는 Converter 생성
     *
     * @param authoritiesClaimName JWT에서 권한 정보를 읽을 클레임 이름
     * @param authorityPrefix 권한 앞에 붙일 접두사
     * @return 설정된 JwtAuthenticationConverter
     */
    public static JwtAuthenticationConverter create(
            String authoritiesClaimName,
            String authorityPrefix
    ) {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter =
                new JwtGrantedAuthoritiesConverter();

        grantedAuthoritiesConverter.setAuthoritiesClaimName(authoritiesClaimName);
        grantedAuthoritiesConverter.setAuthorityPrefix(authorityPrefix);

        JwtAuthenticationConverter jwtAuthenticationConverter =
                new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(
                grantedAuthoritiesConverter
        );

        return jwtAuthenticationConverter;
    }

    /**
     * 기본 설정으로 Converter 생성
     * - 클레임 이름: "roles"
     * - 접두사: "" (Auth Service가 이미 "ROLE_ADMIN" 형태로 보내므로)
     */
    public static JwtAuthenticationConverter createDefault() {
        return create("roles", "");
    }
}