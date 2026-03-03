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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenBlacklistChecker Test")
class TokenBlacklistCheckerTest {

    @Mock
    private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    @InjectMocks
    private TokenBlacklistChecker tokenBlacklistChecker;

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    @Test
    @DisplayName("블랙리스트에 있는 토큰은 true를 반환한다")
    void should_returnTrue_when_tokenIsBlacklisted() {
        String hashedKey = "blacklist:" + hashToken("some-token");
        when(reactiveRedisTemplate.hasKey(hashedKey)).thenReturn(Mono.just(true));

        StepVerifier.create(tokenBlacklistChecker.isBlacklisted("some-token"))
                .expectNext(true)
                .expectComplete()
                .verify();
    }

    @Test
    @DisplayName("블랙리스트에 없는 토큰은 false를 반환한다")
    void should_returnFalse_when_tokenIsNotBlacklisted() {
        String hashedKey = "blacklist:" + hashToken("valid-token");
        when(reactiveRedisTemplate.hasKey(hashedKey)).thenReturn(Mono.just(false));

        StepVerifier.create(tokenBlacklistChecker.isBlacklisted("valid-token"))
                .expectNext(false)
                .expectComplete()
                .verify();
    }

    @Test
    @DisplayName("Redis 장애 시 false를 반환한다 (가용성 우선)")
    void should_returnFalse_when_redisError() {
        String hashedKey = "blacklist:" + hashToken("any-token");
        when(reactiveRedisTemplate.hasKey(hashedKey))
                .thenReturn(Mono.error(new RuntimeException("Redis connection failed")));

        StepVerifier.create(tokenBlacklistChecker.isBlacklisted("any-token"))
                .expectNext(false)
                .expectComplete()
                .verify();
    }

    @Test
    @DisplayName("올바른 키 prefix와 SHA-256 해시로 Redis를 조회한다")
    void should_useCorrectKeyPrefix() {
        String hashedKey = "blacklist:" + hashToken("abc123");
        when(reactiveRedisTemplate.hasKey(hashedKey)).thenReturn(Mono.just(false));

        tokenBlacklistChecker.isBlacklisted("abc123").block();

        verify(reactiveRedisTemplate).hasKey(hashedKey);
    }
}
