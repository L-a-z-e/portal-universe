package com.portal.universe.authservice.auth.repository;

import com.portal.universe.authservice.auth.domain.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoleEntityRepository extends JpaRepository<RoleEntity, Long> {

    Optional<RoleEntity> findByRoleKey(String roleKey);

    boolean existsByRoleKey(String roleKey);

    List<RoleEntity> findByActiveTrue();

    List<RoleEntity> findByServiceScope(String serviceScope);

    List<RoleEntity> findByServiceScopeIsNull();
}
