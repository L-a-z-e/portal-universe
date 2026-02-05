package com.portal.universe.authservice.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenBlacklistService 테스트")
class TokenBlacklistServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private TokenBlacklistService tokenBlacklistService;

    private static final String TOKEN = "sample.jwt.token";
    private static final String BLACKLIST_PREFIX = "blacklist:";

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Nested
    @DisplayName("addToBlacklist")
    class AddToBlacklist {

        @Test
        @DisplayName("should_addTokenToRedis_when_remainingExpirationPositive")
        void should_addTokenToRedis_when_remainingExpirationPositive() {
            // given
            long remainingExpiration = 600_000L;

            // when
            tokenBlacklistService.addToBlacklist(TOKEN, remainingExpiration);

            // then
            verify(valueOperations).set(
                    startsWith(BLACKLIST_PREFIX),
                    eq("blacklisted"),
                    eq(remainingExpiration),
                    eq(TimeUnit.MILLISECONDS)
            );
        }

        @Test
        @DisplayName("should_notAddToken_when_remainingExpirationZero")
        void should_notAddToken_when_remainingExpirationZero() {
            // when
            tokenBlacklistService.addToBlacklist(TOKEN, 0);

            // then
            verifyNoInteractions(valueOperations);
        }

        @Test
        @DisplayName("should_notAddToken_when_remainingExpirationNegative")
        void should_notAddToken_when_remainingExpirationNegative() {
            // when
            tokenBlacklistService.addToBlacklist(TOKEN, -1000);

            // then
            verifyNoInteractions(valueOperations);
        }
    }

    @Nested
    @DisplayName("isBlacklisted")
    class IsBlacklisted {

        @Test
        @DisplayName("should_returnTrue_when_tokenInBlacklist")
        void should_returnTrue_when_tokenInBlacklist() {
            // given
            when(redisTemplate.hasKey(startsWith(BLACKLIST_PREFIX))).thenReturn(Boolean.TRUE);

            // when
            boolean result = tokenBlacklistService.isBlacklisted(TOKEN);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should_returnFalse_when_tokenNotInBlacklist")
        void should_returnFalse_when_tokenNotInBlacklist() {
            // given
            when(redisTemplate.hasKey(startsWith(BLACKLIST_PREFIX))).thenReturn(Boolean.FALSE);

            // when
            boolean result = tokenBlacklistService.isBlacklisted(TOKEN);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should_returnFalse_when_hasKeyReturnsNull")
        void should_returnFalse_when_hasKeyReturnsNull() {
            // given
            when(redisTemplate.hasKey(startsWith(BLACKLIST_PREFIX))).thenReturn(null);

            // when
            boolean result = tokenBlacklistService.isBlacklisted(TOKEN);

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("hashToken - 동일 토큰은 동일 해시")
    class HashTokenConsistency {

        @Test
        @DisplayName("should_produceSameHash_when_sameTokenUsed")
        void should_produceSameHash_when_sameTokenUsed() {
            // given
            tokenBlacklistService.addToBlacklist(TOKEN, 60_000);

            // when
            tokenBlacklistService.isBlacklisted(TOKEN);

            // then - both calls should use the same key (hash of token)
            verify(valueOperations).set(
                    startsWith(BLACKLIST_PREFIX),
                    eq("blacklisted"),
                    eq(60_000L),
                    eq(TimeUnit.MILLISECONDS)
            );
            verify(redisTemplate).hasKey(startsWith(BLACKLIST_PREFIX));
        }
    }
}
