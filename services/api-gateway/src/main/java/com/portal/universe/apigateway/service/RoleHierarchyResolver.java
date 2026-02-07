package com.portal.universe.apigateway.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Auth Service의 RoleHierarchyService를 WebClient로 호출하여
 * Role Hierarchy 상속이 적용된 전체 유효 역할 목록을 반환합니다.
 * 결과는 Redis에 캐싱합니다 (TTL 5분).
 */
@Slf4j
@Service
public class RoleHierarchyResolver {

    private static final String CACHE_PREFIX = "role_hierarchy:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final WebClient webClient;
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RoleHierarchyResolver(
            @Value("${services.auth.url:http://localhost:8081}") String authServiceUrl,
            ReactiveRedisTemplate<String, String> reactiveRedisTemplate) {
        this.webClient = WebClient.builder()
                .baseUrl(authServiceUrl)
                .build();
        this.reactiveRedisTemplate = reactiveRedisTemplate;
    }

    /**
     * 주어진 역할 목록에 대해 상속 포함 전체 유효 역할을 반환합니다.
     * Redis 캐시를 먼저 확인하고, 없으면 auth-service를 호출합니다.
     *
     * @param roles JWT에서 추출한 원본 역할 목록
     * @return 상속 포함 전체 유효 역할 목록
     */
    public Mono<List<String>> resolveEffectiveRoles(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return Mono.just(Collections.emptyList());
        }

        String cacheKey = CACHE_PREFIX + roles.stream().sorted().collect(Collectors.joining(","));

        return reactiveRedisTemplate.opsForValue().get(cacheKey)
                .flatMap(cached -> {
                    try {
                        List<String> result = objectMapper.readValue(cached,
                                objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
                        log.debug("Role hierarchy cache hit for: {}", roles);
                        return Mono.just(result);
                    } catch (Exception e) {
                        log.warn("Failed to parse cached role hierarchy: {}", e.getMessage());
                        return Mono.<List<String>>empty();
                    }
                })
                .switchIfEmpty(fetchAndCache(roles, cacheKey))
                .onErrorResume(e -> {
                    log.warn("Role hierarchy resolution failed, using original roles: {}", e.getMessage());
                    return Mono.just(roles);
                });
    }

    private Mono<List<String>> fetchAndCache(List<String> roles, String cacheKey) {
        String rolesParam = String.join(",", roles);

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/internal/role-hierarchy/effective-roles")
                        .queryParam("roles", rolesParam)
                        .build())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(response -> {
                    JsonNode data = response.get("data");
                    List<String> effectiveRoles = new ArrayList<>();
                    if (data != null && data.isArray()) {
                        data.forEach(node -> effectiveRoles.add(node.asText()));
                    }
                    return effectiveRoles;
                })
                .flatMap(effectiveRoles -> {
                    try {
                        String json = objectMapper.writeValueAsString(effectiveRoles);
                        return reactiveRedisTemplate.opsForValue()
                                .set(cacheKey, json, CACHE_TTL)
                                .thenReturn(effectiveRoles);
                    } catch (Exception e) {
                        log.warn("Failed to cache role hierarchy: {}", e.getMessage());
                        return Mono.just(effectiveRoles);
                    }
                })
                .doOnNext(result -> log.debug("Role hierarchy resolved: {} → {}", roles, result));
    }
}
