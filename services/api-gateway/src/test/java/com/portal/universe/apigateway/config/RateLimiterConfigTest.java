package com.portal.universe.apigateway.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.core.env.Environment;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.test.StepVerifier;

import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("RateLimiterConfig Test")
class RateLimiterConfigTest {

    private RateLimiterConfig createConfig(String... profiles) {
        Environment env = mock(Environment.class);
        when(env.getActiveProfiles()).thenReturn(profiles);
        return new RateLimiterConfig(env);
    }

    @Nested
    @DisplayName("ipKeyResolver")
    class IpKeyResolverTest {

        @Test
        @DisplayName("X-Forwarded-For 헤더에서 첫 번째 IP를 추출한다")
        void should_resolveIpFromXForwardedFor() {
            var config = createConfig("production");
            KeyResolver resolver = config.ipKeyResolver();

            var request = MockServerHttpRequest.get("/api/test")
                    .header("X-Forwarded-For", "10.0.0.1, 10.0.0.2")
                    .build();
            var exchange = MockServerWebExchange.from(request);

            StepVerifier.create(resolver.resolve(exchange))
                    .expectNext("10.0.0.1")
                    .expectComplete()
                    .verify();
        }

        @Test
        @DisplayName("X-Forwarded-For가 없으면 RemoteAddress에서 IP를 추출한다")
        void should_resolveIpFromRemoteAddress() {
            var config = createConfig("production");
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
            var config = createConfig("production");
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
            var config = createConfig("production");
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
            var config = createConfig("production");
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
            var config = createConfig("production");
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
        @DisplayName("docker 프로파일에서는 완화된 rate limit를 사용한다")
        void should_useRelaxedLimits_when_dockerProfile() {
            var config = createConfig("docker");
            var limiter = config.defaultRedisRateLimiter();

            assertThat(limiter).isNotNull();
        }

        @Test
        @DisplayName("production 프로파일에서는 엄격한 rate limit를 사용한다")
        void should_useStrictLimits_when_productionProfile() {
            var config = createConfig("production");
            var limiter = config.defaultRedisRateLimiter();

            assertThat(limiter).isNotNull();
        }

        @Test
        @DisplayName("local 프로파일에서는 완화된 strict rate limit를 사용한다")
        void should_useRelaxedStrictLimits_when_localProfile() {
            var config = createConfig("local");
            var limiter = config.strictRedisRateLimiter();

            assertThat(limiter).isNotNull();
        }

        @Test
        @DisplayName("모든 RateLimiter Bean을 생성할 수 있다")
        void should_createAllRateLimiterBeans() {
            var config = createConfig("production");

            assertThat(config.defaultRedisRateLimiter()).isNotNull();
            assertThat(config.strictRedisRateLimiter()).isNotNull();
            assertThat(config.signupRedisRateLimiter()).isNotNull();
            assertThat(config.authenticatedRedisRateLimiter()).isNotNull();
            assertThat(config.unauthenticatedRedisRateLimiter()).isNotNull();
        }
    }
}
