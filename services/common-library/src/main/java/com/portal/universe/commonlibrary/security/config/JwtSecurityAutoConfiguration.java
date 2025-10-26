package com.portal.universe.commonlibrary.security.config;

import com.portal.universe.commonlibrary.security.converter.JwtAuthenticationConverterAdapter;
import com.portal.universe.commonlibrary.security.converter.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import reactor.core.publisher.Mono;

/**
 * JWT 관련 보안 설정을 자동으로 구성하는 Spring Boot Auto-Configuration 클래스입니다.
 * 이 클래스는 `resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 파일에 등록되어
 * Spring Boot 시작 시 자동으로 로드됩니다.
 *
 * ### 동작 방식
 * 1. 애플리케이션의 타입(Servlet 또는 Reactive)을 감지합니다.
 * 2. 해당 환경에 맞는 JWT 권한 변환기(Converter) Bean을 자동으로 등록합니다.
 * 3. 만약 각 마이크로서비스에서 동일한 타입의 Bean을 직접 정의한 경우, 이 자동 설정은 동작하지 않습니다. (`@ConditionalOnMissingBean`)
 */
@AutoConfiguration
@ConditionalOnClass(JwtAuthenticationConverter.class) // 클래스패스에 관련 클래스가 있을 때만 동작
public class JwtSecurityAutoConfiguration {

    /**
     * **Servlet (Spring MVC) 환경**을 위한 {@link JwtAuthenticationConverter} Bean을 생성합니다.
     * JWT의 `roles` 클레임을 Spring Security의 `GrantedAuthority`로 변환하는 역할을 합니다.
     *
     * @return {@link JwtAuthenticationConverterAdapter}를 통해 생성된 기본 JWT 컨버터
     */
    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnMissingBean(JwtAuthenticationConverter.class)
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        return JwtAuthenticationConverterAdapter.createDefault();
    }

    /**
     * **Reactive (Spring WebFlux) 환경**을 위한 JWT 권한 변환기 Bean을 생성합니다.
     * API Gateway와 같은 Reactive 기반 서비스에서 사용됩니다.
     *
     * @return {@link ReactiveJwtAuthenticationConverterAdapter} 인스턴스
     */
    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    @ConditionalOnMissingBean(name = "reactiveJwtAuthenticationConverter")
    public Converter<Jwt, Mono<AbstractAuthenticationToken>> reactiveJwtAuthenticationConverter() {
        return new ReactiveJwtAuthenticationConverterAdapter();
    }
}
