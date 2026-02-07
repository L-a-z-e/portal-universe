package com.portal.universe.authservice.auth.service;

import com.portal.universe.authservice.auth.domain.RoleEntity;
import com.portal.universe.authservice.auth.repository.RoleEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * DB parentRole 기반 Role Hierarchy를 구성합니다.
 * Gateway에서 호출하여 유효 역할 목록을 계산합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoleHierarchyService {

    private final RoleEntityRepository roleEntityRepository;

    /**
     * 주어진 역할 목록에 대해 상속된 모든 유효 역할을 반환합니다.
     * 예: [ROLE_SHOPPING_ADMIN] → [ROLE_SHOPPING_ADMIN, ROLE_SHOPPING_SELLER, ROLE_USER]
     *
     * @param roleKeys 사용자에게 직접 할당된 역할 키 목록
     * @return 상속 포함 전체 유효 역할 키 목록
     */
    public List<String> resolveEffectiveRoles(List<String> roleKeys) {
        Set<String> effectiveRoles = new LinkedHashSet<>(roleKeys);

        for (String roleKey : roleKeys) {
            collectInheritedRoles(roleKey, effectiveRoles);
        }

        return new ArrayList<>(effectiveRoles);
    }

    /**
     * 전체 역할 계층 구조를 문자열로 반환합니다 (Spring Security RoleHierarchy 포맷).
     * 예: "ROLE_SUPER_ADMIN > ROLE_SHOPPING_ADMIN\nROLE_SHOPPING_ADMIN > ROLE_SHOPPING_SELLER\n..."
     */
    public String getRoleHierarchyExpression() {
        List<RoleEntity> allRoles = roleEntityRepository.findByActiveTrue();
        StringBuilder sb = new StringBuilder();

        for (RoleEntity role : allRoles) {
            if (role.getParentRole() != null) {
                // parentRole이 "이 역할보다 하위"가 아니라 "이 역할의 부모(하위)"
                // 계층: SHOPPING_ADMIN → SHOPPING_SELLER → USER
                // parentRole은 하위 역할을 가리킴
                // Spring RoleHierarchy: 상위 > 하위
                // 여기서는 role의 parentRole이 하위이므로: role > parentRole
                sb.append(role.getRoleKey())
                        .append(" > ")
                        .append(role.getParentRole().getRoleKey())
                        .append("\n");
            }
        }

        return sb.toString().trim();
    }

    private void collectInheritedRoles(String roleKey, Set<String> collected) {
        RoleEntity role = roleEntityRepository.findByRoleKey(roleKey).orElse(null);
        if (role == null) {
            return;
        }

        RoleEntity parent = role.getParentRole();
        while (parent != null) {
            if (!collected.add(parent.getRoleKey())) {
                break; // 순환 방지
            }
            parent = parent.getParentRole();
        }
    }
}
