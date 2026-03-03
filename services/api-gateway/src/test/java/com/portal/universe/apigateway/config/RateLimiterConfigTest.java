package com.portal.universe.apigateway.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.test.StepVerifier;

import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RateLimiterConfig Test")
class RateLimiterConfigTest {

    private RateLimiterConfig createConfig(int defaultRate, int defaultBurst,
                                           int strictRate, int strictBurst,
                                           int signupRate, int signupBurst,
                                           int authRate, int authBurst,
                                           int unauthRate, int unauthBurst) {
        RateLimiterConfig config = new RateLimiterConfig();
        ReflectionTestUtils.setField(config, "defaultReplenishRate", defaultRate);
        ReflectionTestUtils.setField(config, "defaultBurstCapacity", defaultBurst);
        ReflectionTestUtils.setField(config, "strictReplenishRate", strictRate);
        ReflectionTestUtils.setField(config, "strictBurstCapacity", strictBurst);
        ReflectionTestUtils.setField(config, "signupReplenishRate", signupRate);
        ReflectionTestUtils.setField(config, "signupBurstCapacity", signupBurst);
        ReflectionTestUtils.setField(config, "authenticatedReplenishRate", authRate);
        ReflectionTestUtils.setField(config, "authenticatedBurstCapacity", authBurst);
        ReflectionTestUtils.setField(config, "unauthenticatedReplenishRate", unauthRate);
        ReflectionTestUtils.setField(config, "unauthenticatedBurstCapacity", unauthBurst);
        return config;
    }

    private RateLimiterConfig createProductionConfig() {
        return createConfig(10, 20, 1, 5, 1, 3, 2, 100, 1, 30);
    }

    private RateLimiterConfig createRelaxedConfig() {
        return createConfig(50, 200, 20, 50, 20, 50, 50, 500, 50, 200);
    }

    @Nested
    @DisplayName("ipKeyResolver")
    class IpKeyResolverTest {

        @Test
        @DisplayName("X-Forwarded-For 헤더에서 maxTrustedIndex(1) 기준으로 IP를 추출한다")
        void should_resolveIpFromXForwardedFor() {
            var config = createProductionConfig();
            KeyResolver resolver = config.ipKeyResolver();

            // maxTrustedIndex(1): index = size - 1 = 1 → "10.0.0.2"
            var request = MockServerHttpRequest.get("/api/test")
                    .header("X-Forwarded-For", "10.0.0.1, 10.0.0.2")
                    .build();
            var exchange = MockServerWebExchange.from(request);

            StepVerifier.create(resolver.resolve(exchange))
                    .expectNext("10.0.0.2")
                    .expectComplete()
                    .verify();
        }

        @Test
        @DisplayName("X-Forwarded-For가 없으면 RemoteAddress에서 IP를 추출한다")
        void should_resolveIpFromRemoteAddress() {
            var config = createProductionConfig();
            KeyResolver resolver = config.ipKeyResolver();

            var request = MockServerHttpRequest.get("/api/test")
                    .remoteAddress(new InetSocketAddress("192.168.1.1", 8080))
                    .build();
            var exchange = MockServerWebExchange.from(request);

            StepVerifier.create(resolver.resolve(exchange))
                    .expectNext("192.168.1.1")
                    .expectComplete()
                    .verify();
        }

        @Test
        @DisplayName("IP를 얻을 수 없으면 unknown을 반환한다")
        void should_resolveUnknown_when_noIp() {
            var config = createProductionConfig();
            KeyResolver resolver = config.ipKeyResolver();

            var request = MockServerHttpRequest.get("/api/test").build();
            var exchange = MockServerWebExchange.from(request);

            StepVerifier.create(resolver.resolve(exchange))
                    .expectNext("unknown")
                    .expectComplete()
                    .verify();
        }
    }

    @Nested
    @DisplayName("userKeyResolver")
    class UserKeyResolverTest {

        @Test
        @DisplayName("X-User-Id 헤더가 있으면 user: prefix로 키를 생성한다")
        void should_resolveUserId_when_authenticated() {
            var config = createProductionConfig();
            KeyResolver resolver = config.userKeyResolver();

            var request = MockServerHttpRequest.get("/api/test")
                    .header("X-User-Id", "user1")
                    .build();
            var exchange = MockServerWebExchange.from(request);

            StepVerifier.create(resolver.resolve(exchange))
                    .expectNext("user:user1")
                    .expectComplete()
                    .verify();
        }

        @Test
        @DisplayName("X-User-Id가 없으면 IP 기반으로 폴백한다")
        void should_fallbackToIp_when_notAuthenticated() {
            var config = createProductionConfig();
            KeyResolver resolver = config.userKeyResolver();

            var request = MockServerHttpRequest.get("/api/test")
                    .remoteAddress(new InetSocketAddress("10.0.0.1", 8080))
                    .build();
            var exchange = MockServerWebExchange.from(request);

            StepVerifier.create(resolver.resolve(exchange))
                    .expectNext("10.0.0.1")
                    .expectComplete()
                    .verify();
        }
    }

    @Nested
    @DisplayName("compositeKeyResolver")
    class CompositeKeyResolverTest {

        @Test
        @DisplayName("IP + 경로 조합으로 키를 생성한다")
        void should_resolveCompositeKey() {
            var config = createProductionConfig();
            KeyResolver resolver = config.compositeKeyResolver();

            var request = MockServerHttpRequest.get("/api/test")
                    .header("X-Forwarded-For", "10.0.0.1")
                    .build();
            var exchange = MockServerWebExchange.from(request);

            StepVerifier.create(resolver.resolve(exchange))
                    .expectNext("10.0.0.1:/api/test")
                    .expectComplete()
                    .verify();
        }
    }

    @Nested
    @DisplayName("RateLimiter Beans")
    class RateLimiterBeans {

        @Test
        @DisplayName("프로덕션 설정으로 모든 RateLimiter Bean을 생성할 수 있다")
        void should_createAllRateLimiterBeans_withProductionConfig() {
            var config = createProductionConfig();

            assertThat(config.defaultRedisRateLimiter()).isNotNull();
            assertThat(config.strictRedisRateLimiter()).isNotNull();
            assertThat(config.signupRedisRateLimiter()).isNotNull();
            assertThat(config.authenticatedRedisRateLimiter()).isNotNull();
            assertThat(config.unauthenticatedRedisRateLimiter()).isNotNull();
        }

        @Test
        @DisplayName("완화된 설정으로 모든 RateLimiter Bean을 생성할 수 있다")
        void should_createAllRateLimiterBeans_withRelaxedConfig() {
            var config = createRelaxedConfig();

            assertThat(config.defaultRedisRateLimiter()).isNotNull();
            assertThat(config.strictRedisRateLimiter()).isNotNull();
            assertThat(config.signupRedisRateLimiter()).isNotNull();
            assertThat(config.authenticatedRedisRateLimiter()).isNotNull();
            assertThat(config.unauthenticatedRedisRateLimiter()).isNotNull();
        }
    }
}
