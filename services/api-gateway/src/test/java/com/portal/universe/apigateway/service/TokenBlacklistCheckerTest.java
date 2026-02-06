package com.portal.universe.apigateway.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenBlacklistChecker Test")
class TokenBlacklistCheckerTest {

    @Mock
    private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    @InjectMocks
    private TokenBlacklistChecker tokenBlacklistChecker;

    @Test
    @DisplayName("블랙리스트에 있는 토큰은 true를 반환한다")
    void should_returnTrue_when_tokenIsBlacklisted() {
        when(reactiveRedisTemplate.hasKey("blacklist:some-token")).thenReturn(Mono.just(true));

        StepVerifier.create(tokenBlacklistChecker.isBlacklisted("some-token"))
                .expectNext(true)
                .expectComplete()
                .verify();
    }

    @Test
    @DisplayName("블랙리스트에 없는 토큰은 false를 반환한다")
    void should_returnFalse_when_tokenIsNotBlacklisted() {
        when(reactiveRedisTemplate.hasKey("blacklist:valid-token")).thenReturn(Mono.just(false));

        StepVerifier.create(tokenBlacklistChecker.isBlacklisted("valid-token"))
                .expectNext(false)
                .expectComplete()
                .verify();
    }

    @Test
    @DisplayName("Redis 장애 시 false를 반환한다 (가용성 우선)")
    void should_returnFalse_when_redisError() {
        when(reactiveRedisTemplate.hasKey("blacklist:any-token"))
                .thenReturn(Mono.error(new RuntimeException("Redis connection failed")));

        StepVerifier.create(tokenBlacklistChecker.isBlacklisted("any-token"))
                .expectNext(false)
                .expectComplete()
                .verify();
    }

    @Test
    @DisplayName("올바른 키 prefix로 Redis를 조회한다")
    void should_useCorrectKeyPrefix() {
        when(reactiveRedisTemplate.hasKey("blacklist:abc123")).thenReturn(Mono.just(false));

        tokenBlacklistChecker.isBlacklisted("abc123").block();

        verify(reactiveRedisTemplate).hasKey("blacklist:abc123");
    }
}
