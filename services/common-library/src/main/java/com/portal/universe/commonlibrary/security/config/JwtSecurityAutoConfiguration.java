package com.portal.universe.commonlibrary.security.config;

import com.portal.universe.commonlibrary.security.converter.JwtAuthenticationConverterAdapter;
import com.portal.universe.commonlibrary.security.converter.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import reactor.core.publisher.Mono;

/**
 * JWT 보안 설정을 자동으로 구성하는 Auto-Configuration
 *
 * 동작 방식:
 * 1. Servlet 환경 감지 → JwtAuthenticationConverter Bean 자동 등록
 * 2. Reactive 환경 감지 → ReactiveJwtAuthenticationConverter Bean 자동 등록
 * 3. 각 서비스에서 별도 Bean 정의 시 자동 설정 무시 (@ConditionalOnMissingBean)
 */
@AutoConfiguration
public class JwtSecurityAutoConfiguration {

    /**
     * Servlet(MVC) 환경용 JwtAuthenticationConverter
     *
     * 자동 적용 조건:
     * - WebApplicationType이 SERVLET일 때
     * - JwtAuthenticationConverter Bean이 없을 때
     */
    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnMissingBean(JwtAuthenticationConverter.class)
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        return JwtAuthenticationConverterAdapter.createDefault();
    }

    /**
     * Reactive(WebFlux) 환경용 Converter
     *
     * 자동 적용 조건:
     * - WebApplicationType이 REACTIVE일 때
     * - reactiveJwtAuthenticationConverter Bean이 없을 때
     */
    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    @ConditionalOnMissingBean(name = "reactiveJwtAuthenticationConverter")
    public Converter<Jwt, Mono<AbstractAuthenticationToken>> reactiveJwtAuthenticationConverter() {
        return new ReactiveJwtAuthenticationConverterAdapter();
    }
}