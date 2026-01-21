package com.portal.universe.authservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 로그아웃된 Access Token을 블랙리스트로 관리하는 서비스입니다.
 * Redis 키 패턴: blacklist:{accessToken}
 *
 * 블랙리스트에 추가된 토큰은 남은 만료 시간 동안만 저장되며,
 * 만료 시간이 지나면 자동으로 삭제됩니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String BLACKLIST_PREFIX = "blacklist:";

    /**
     * Access Token을 블랙리스트에 추가합니다.
     * TTL은 토큰의 남은 만료 시간으로 설정됩니다.
     *
     * @param token Access Token
     * @param remainingExpiration 남은 만료 시간 (밀리초)
     */
    public void addToBlacklist(String token, long remainingExpiration) {
        if (remainingExpiration <= 0) {
            log.warn("Cannot blacklist expired token");
            return;
        }

        String key = BLACKLIST_PREFIX + token;
        redisTemplate.opsForValue().set(
                key,
                "blacklisted",
                remainingExpiration,
                TimeUnit.MILLISECONDS
        );
        log.info("Token added to blacklist with TTL: {}ms", remainingExpiration);
    }

    /**
     * Access Token이 블랙리스트에 있는지 확인합니다.
     *
     * @param token Access Token
     * @return 블랙리스트에 있으면 true, 그렇지 않으면 false
     */
    public boolean isBlacklisted(String token) {
        String key = BLACKLIST_PREFIX + token;
        Boolean exists = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }
}
