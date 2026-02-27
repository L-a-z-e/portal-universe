package com.portal.universe.apigateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.support.ipresolver.XForwardedRemoteAddressResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.Arrays;

/**
 * Redis 기반 Rate Limiting 설정을 담당하는 클래스입니다.
 * Spring Cloud Gateway의 RedisRateLimiter를 사용하여 다양한 전략의 속도 제한을 구현합니다.
 *
 * Rate Limiting 전략:
 * - IP 기반: 클라이언트 IP 주소로 제한
 * - User 기반: 인증된 사용자 ID (X-User-Id 헤더)로 제한
 * - Composite 기반: IP + 엔드포인트 조합으로 제한
 *
 * Token Bucket Algorithm:
 * - replenishRate: 초당 토큰 충전 속도 (sustained rate)
 * - burstCapacity: 최대 버스트 용량 (peak rate)
 * - requestedTokens: 요청당 소비 토큰 수 (기본값 1)
 */
@Slf4j
@Configuration
public class RateLimiterConfig {

    private final boolean isRelaxedRateLimiting;

    public RateLimiterConfig(Environment environment) {
        var profiles = Arrays.asList(environment.getActiveProfiles());
        // docker, local 프로파일에서는 개발/테스트를 위해 완화된 Rate Limiting 적용
        this.isRelaxedRateLimiting = profiles.contains("docker") || profiles.contains("local");
        if (isRelaxedRateLimiting) {
            log.info("Development profile detected (docker/local) - using relaxed rate limits for testing");
        }
    }

    /**
     * IP 주소 기반 KeyResolver (기본값)
     * GlobalLoggingFilter와 동일한 maxTrustedIndex(1)을 사용하여
     * X-Forwarded-For 스푸핑을 방지합니다.
     */
    @Bean
    @Primary
    public KeyResolver ipKeyResolver() {
        XForwardedRemoteAddressResolver resolver = XForwardedRemoteAddressResolver.maxTrustedIndex(1);
        return exchange -> {
            InetSocketAddress remoteAddress = resolver.resolve(exchange);
            String clientIp = (remoteAddress != null && remoteAddress.getAddress() != null)
                    ? remoteAddress.getAddress().getHostAddress()
                    : "unknown";

            log.debug("Rate Limit Key (IP): {}", clientIp);
            return Mono.just(clientIp);
        };
    }

    /**
     * 사용자 ID 기반 KeyResolver
     * JwtAuthenticationFilter에서 추가한 X-User-Id 헤더 사용
     * 인증되지 않은 요청은 IP 기반으로 폴백
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");

            if (userId != null && !userId.isEmpty()) {
                log.debug("Rate Limit Key (User): {}", userId);
                return Mono.just("user:" + userId);
            }

            // 인증되지 않은 요청은 IP 기반으로 폴백
            return ipKeyResolver().resolve(exchange);
        };
    }

    /**
     * 복합 키 기반 KeyResolver
     * IP + 엔드포인트 조합으로 특정 경로별 제한 가능
     * 예: 로그인 API는 IP당 별도 제한
     */
    @Bean
    public KeyResolver compositeKeyResolver() {
        return exchange -> {
            String path = exchange.getRequest().getPath().value();

            return ipKeyResolver().resolve(exchange)
                .map(ip -> ip + ":" + path);
        };
    }

    /**
     * 기본 RedisRateLimiter Bean
     * 일반 API 요청에 대한 기본값 설정
     *
     * replenishRate: 10 req/sec (지속 속도)
     * burstCapacity: 20 (버스트 용량)
     */
    @Bean
    @Primary
    public RedisRateLimiter defaultRedisRateLimiter() {
        return isRelaxedRateLimiting
            ? new RedisRateLimiter(50, 200, 1)
            : new RedisRateLimiter(10, 20, 1);
    }

    /**
     * 로그인 API용 엄격한 Rate Limiter
     * Brute Force 공격 방어
     *
     * replenishRate: 1 req/sec (지속 속도)
     * burstCapacity: 5 (최대 5회 버스트 후 초당 1회로 제한)
     */
    @Bean
    public RedisRateLimiter strictRedisRateLimiter() {
        return isRelaxedRateLimiting
            ? new RedisRateLimiter(20, 50, 1)
            : new RedisRateLimiter(1, 5, 1);
    }

    /**
     * 회원가입 API용 Rate Limiter
     *
     * replenishRate: 1 req/sec (지속 속도)
     * burstCapacity: 3 (최대 3회 버스트 후 초당 1회로 제한)
     */
    @Bean
    public RedisRateLimiter signupRedisRateLimiter() {
        return isRelaxedRateLimiting
            ? new RedisRateLimiter(20, 50, 1)
            : new RedisRateLimiter(1, 3, 1);
    }

    /**
     * 인증된 사용자용 관대한 Rate Limiter
     *
     * replenishRate: 2 req/sec (지속 속도)
     * burstCapacity: 100 (버스트 허용, 지속적으로는 초당 2회)
     */
    @Bean
    public RedisRateLimiter authenticatedRedisRateLimiter() {
        return isRelaxedRateLimiting
            ? new RedisRateLimiter(50, 500, 1)
            : new RedisRateLimiter(2, 100, 1);
    }

    /**
     * 비인증 사용자용 제한적인 Rate Limiter
     *
     * replenishRate: 1 req/sec (지속 속도)
     * burstCapacity: 30 (버스트 허용, 지속적으로는 초당 1회)
     */
    @Bean
    public RedisRateLimiter unauthenticatedRedisRateLimiter() {
        return isRelaxedRateLimiting
            ? new RedisRateLimiter(50, 200, 1)
            : new RedisRateLimiter(1, 30, 1);
    }
}
