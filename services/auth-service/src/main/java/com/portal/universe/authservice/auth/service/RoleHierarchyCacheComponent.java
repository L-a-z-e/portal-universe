package com.portal.universe.authservice.auth.service;

import com.portal.universe.authservice.auth.domain.RoleInclude;
import com.portal.universe.authservice.auth.repository.RoleIncludeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Role Hierarchy 캐시 처리 전담 컴포넌트.
 * @Cacheable/@CacheEvict가 AOP 프록시를 통해 정상 동작하도록
 * 비즈니스 서비스(RoleHierarchyService)와 구조적으로 분리합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoleHierarchyCacheComponent {

    private final RoleIncludeRepository roleIncludeRepository;

    /**
     * 전체 역할 계층 DAG를 Map 형태로 반환합니다.
     * Redis에 캐시되며, 역할 구조 변경 시 evictHierarchyCache()로 무효화합니다.
     * key: roleKey, value: direct includes
     */
    @Cacheable(value = "roleHierarchy", key = "'graph'")
    public Map<String, List<String>> getHierarchyGraph() {
        log.info("Loading role hierarchy graph from DB");
        List<RoleInclude> allIncludes = roleIncludeRepository.findAllWithRoles();
        return allIncludes.stream()
                .collect(Collectors.groupingBy(
                        ri -> ri.getRole().getRoleKey(),
                        Collectors.mapping(ri -> ri.getIncludedRole().getRoleKey(), Collectors.toList())
                ));
    }

    /**
     * 역할 계층 캐시를 무효화합니다.
     * 역할 구조가 변경될 때 (include 추가/제거) 호출해야 합니다.
     */
    @CacheEvict(value = "roleHierarchy", allEntries = true)
    public void evictHierarchyCache() {
        log.info("Role hierarchy cache evicted");
    }
}
