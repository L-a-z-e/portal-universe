package com.portal.universe.authservice.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * role_includes 테이블 기반 DAG Role Hierarchy를 구성합니다.
 * BFS로 effective roles를 계산하고, cycle detection을 수행합니다.
 * 캐시 처리는 RoleHierarchyCacheComponent에 위임합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoleHierarchyService {

    private final RoleHierarchyCacheComponent cacheComponent;

    /**
     * 주어진 역할 목록에 대해 DAG를 BFS 탐색하여 모든 유효 역할을 반환합니다.
     * 캐시된 계층 그래프를 사용하여 DB 쿼리 없이 메모리에서 해결합니다.
     * 예: [ROLE_SUPER_ADMIN] → [ROLE_SUPER_ADMIN, ROLE_SHOPPING_ADMIN, ROLE_BLOG_ADMIN, ...]
     */
    public List<String> resolveEffectiveRoles(List<String> roleKeys) {
        Map<String, List<String>> graph = cacheComponent.getHierarchyGraph();

        Set<String> visited = new LinkedHashSet<>(roleKeys);
        Queue<String> queue = new LinkedList<>(roleKeys);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            List<String> includes = graph.getOrDefault(current, List.of());
            for (String includedKey : includes) {
                if (visited.add(includedKey)) {
                    queue.add(includedKey);
                }
            }
        }

        return new ArrayList<>(visited);
    }

    /**
     * candidateIncludeKey를 roleKey에 추가했을 때 cycle이 발생하는지 검사합니다.
     * candidateIncludeKey에서 BFS로 roleKey에 도달 가능하면 cycle.
     */
    public boolean wouldCreateCycle(String roleKey, String candidateIncludeKey) {
        Map<String, List<String>> graph = cacheComponent.getHierarchyGraph();

        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        queue.add(candidateIncludeKey);
        visited.add(candidateIncludeKey);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (current.equals(roleKey)) {
                return true;
            }
            List<String> includes = graph.getOrDefault(current, List.of());
            for (String includedKey : includes) {
                if (visited.add(includedKey)) {
                    queue.add(includedKey);
                }
            }
        }

        return false;
    }

    /**
     * 전체 역할 계층 DAG를 Map 형태로 반환합니다.
     * 캐시된 그래프를 통해 DB 쿼리 없이 조회합니다.
     */
    public Map<String, List<String>> getHierarchyGraph() {
        return cacheComponent.getHierarchyGraph();
    }

    /**
     * 역할 계층 캐시를 무효화합니다.
     * RbacService에서 역할 구조 변경 시 호출합니다.
     */
    public void evictHierarchyCache() {
        cacheComponent.evictHierarchyCache();
    }

    /**
     * @deprecated Gateway에서 미사용. JWT effectiveRoles claim으로 대체.
     */
    @Deprecated
    public String getRoleHierarchyExpression() {
        Map<String, List<String>> graph = cacheComponent.getHierarchyGraph();
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<String, List<String>> entry : graph.entrySet()) {
            for (String included : entry.getValue()) {
                sb.append(entry.getKey()).append(" > ").append(included).append("\n");
            }
        }

        return sb.toString().trim();
    }
}
