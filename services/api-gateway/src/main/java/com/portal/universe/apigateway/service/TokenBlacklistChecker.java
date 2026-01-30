package com.portal.universe.apigateway.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Gateway에서 로그아웃된 토큰(블랙리스트)을 확인하는 서비스입니다.
 * Auth-service의 TokenBlacklistService와 동일한 Redis 키 패턴을 사용합니다.
 * Redis 키 패턴: blacklist:{accessToken}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistChecker {

    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private static final String BLACKLIST_PREFIX = "blacklist:";

    /**
     * 토큰이 블랙리스트에 등록되어 있는지 확인합니다.
     *
     * @param token Access Token
     * @return 블랙리스트에 있으면 true
     */
    public Mono<Boolean> isBlacklisted(String token) {
        String key = BLACKLIST_PREFIX + token;
        return reactiveRedisTemplate.hasKey(key)
                .doOnNext(blacklisted -> {
                    if (Boolean.TRUE.equals(blacklisted)) {
                        log.debug("Token is blacklisted");
                    }
                })
                .onErrorResume(e -> {
                    log.error("Failed to check token blacklist: {}", e.getMessage());
                    // Redis 장애 시 토큰을 허용 (가용성 우선)
                    return Mono.just(false);
                });
    }
}
