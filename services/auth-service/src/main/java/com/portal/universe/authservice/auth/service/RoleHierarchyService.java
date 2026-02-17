package com.portal.universe.authservice.auth.service;

import com.portal.universe.authservice.auth.domain.RoleInclude;
import com.portal.universe.authservice.auth.repository.RoleIncludeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * role_includes 테이블 기반 DAG Role Hierarchy를 구성합니다.
 * BFS로 effective roles를 계산하고, cycle detection을 수행합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoleHierarchyService {

    private final RoleIncludeRepository roleIncludeRepository;

    /**
     * 주어진 역할 목록에 대해 DAG를 BFS 탐색하여 모든 유효 역할을 반환합니다.
     * 예: [ROLE_SUPER_ADMIN] → [ROLE_SUPER_ADMIN, ROLE_SHOPPING_ADMIN, ROLE_BLOG_ADMIN, ROLE_SHOPPING_SELLER, ROLE_USER, ROLE_GUEST]
     */
    public List<String> resolveEffectiveRoles(List<String> roleKeys) {
        Set<String> visited = new LinkedHashSet<>(roleKeys);
        Queue<String> queue = new LinkedList<>(roleKeys);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            List<RoleInclude> includes = roleIncludeRepository.findByRoleRoleKey(current);
            for (RoleInclude include : includes) {
                String includedKey = include.getIncludedRole().getRoleKey();
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
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        queue.add(candidateIncludeKey);
        visited.add(candidateIncludeKey);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (current.equals(roleKey)) {
                return true;
            }
            List<RoleInclude> includes = roleIncludeRepository.findByRoleRoleKey(current);
            for (RoleInclude include : includes) {
                String includedKey = include.getIncludedRole().getRoleKey();
                if (visited.add(includedKey)) {
                    queue.add(includedKey);
                }
            }
        }

        return false;
    }

    /**
     * 전체 역할 계층 DAG를 Map 형태로 반환합니다.
     * key: roleKey, value: direct includes
     */
    public Map<String, List<String>> getHierarchyGraph() {
        List<RoleInclude> allIncludes = roleIncludeRepository.findAllWithRoles();
        return allIncludes.stream()
                .collect(Collectors.groupingBy(
                        ri -> ri.getRole().getRoleKey(),
                        Collectors.mapping(ri -> ri.getIncludedRole().getRoleKey(), Collectors.toList())
                ));
    }

    /**
     * @deprecated Gateway에서 미사용. JWT effectiveRoles claim으로 대체.
     */
    @Deprecated
    public String getRoleHierarchyExpression() {
        List<RoleInclude> allIncludes = roleIncludeRepository.findAllWithRoles();
        StringBuilder sb = new StringBuilder();

        for (RoleInclude include : allIncludes) {
            sb.append(include.getRole().getRoleKey())
                    .append(" > ")
                    .append(include.getIncludedRole().getRoleKey())
                    .append("\n");
        }

        return sb.toString().trim();
    }
}
