package com.portal.universe.apigateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.support.ipresolver.XForwardedRemoteAddressResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

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
 *
 * 환경변수로 Rate Limit 값을 외부에서 조정할 수 있습니다.
 * 부하 테스트 시 K8s ConfigMap의 환경변수를 변경하여 rate limit을 완화할 수 있습니다.
 */
@Slf4j
@Configuration
public class RateLimiterConfig {

    @Value("${rate-limiter.default.replenish-rate:10}")
    private int defaultReplenishRate;
    @Value("${rate-limiter.default.burst-capacity:20}")
    private int defaultBurstCapacity;

    @Value("${rate-limiter.strict.replenish-rate:1}")
    private int strictReplenishRate;
    @Value("${rate-limiter.strict.burst-capacity:5}")
    private int strictBurstCapacity;

    @Value("${rate-limiter.signup.replenish-rate:1}")
    private int signupReplenishRate;
    @Value("${rate-limiter.signup.burst-capacity:3}")
    private int signupBurstCapacity;

    @Value("${rate-limiter.authenticated.replenish-rate:2}")
    private int authenticatedReplenishRate;
    @Value("${rate-limiter.authenticated.burst-capacity:100}")
    private int authenticatedBurstCapacity;

    @Value("${rate-limiter.unauthenticated.replenish-rate:1}")
    private int unauthenticatedReplenishRate;
    @Value("${rate-limiter.unauthenticated.burst-capacity:30}")
    private int unauthenticatedBurstCapacity;

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

    @Bean
    @Primary
    public RedisRateLimiter defaultRedisRateLimiter() {
        log.info("Default Rate Limiter: replenishRate={}, burstCapacity={}", defaultReplenishRate, defaultBurstCapacity);
        return new RedisRateLimiter(defaultReplenishRate, defaultBurstCapacity, 1);
    }

    /**
     * 로그인 API용 엄격한 Rate Limiter
     * Brute Force 공격 방어
     */
    @Bean
    public RedisRateLimiter strictRedisRateLimiter() {
        log.info("Strict Rate Limiter: replenishRate={}, burstCapacity={}", strictReplenishRate, strictBurstCapacity);
        return new RedisRateLimiter(strictReplenishRate, strictBurstCapacity, 1);
    }

    @Bean
    public RedisRateLimiter signupRedisRateLimiter() {
        log.info("Signup Rate Limiter: replenishRate={}, burstCapacity={}", signupReplenishRate, signupBurstCapacity);
        return new RedisRateLimiter(signupReplenishRate, signupBurstCapacity, 1);
    }

    @Bean
    public RedisRateLimiter authenticatedRedisRateLimiter() {
        log.info("Authenticated Rate Limiter: replenishRate={}, burstCapacity={}", authenticatedReplenishRate, authenticatedBurstCapacity);
        return new RedisRateLimiter(authenticatedReplenishRate, authenticatedBurstCapacity, 1);
    }

    @Bean
    public RedisRateLimiter unauthenticatedRedisRateLimiter() {
        log.info("Unauthenticated Rate Limiter: replenishRate={}, burstCapacity={}", unauthenticatedReplenishRate, unauthenticatedBurstCapacity);
        return new RedisRateLimiter(unauthenticatedReplenishRate, unauthenticatedBurstCapacity, 1);
    }
}