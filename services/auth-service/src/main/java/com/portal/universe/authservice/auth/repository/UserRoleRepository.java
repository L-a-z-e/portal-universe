package com.portal.universe.authservice.auth.repository;

import com.portal.universe.authservice.auth.domain.RoleEntity;
import com.portal.universe.authservice.auth.domain.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    List<UserRole> findByUserId(String userId);

    @Query("SELECT ur FROM UserRole ur JOIN FETCH ur.role WHERE ur.userId = :userId")
    List<UserRole> findByUserIdWithRole(@Param("userId") String userId);

    @Query("SELECT ur.role.roleKey FROM UserRole ur WHERE ur.userId = :userId " +
            "AND (ur.expiresAt IS NULL OR ur.expiresAt > CURRENT_TIMESTAMP)")
    List<String> findActiveRoleKeysByUserId(@Param("userId") String userId);

    boolean existsByUserIdAndRole(String userId, RoleEntity role);

    Optional<UserRole> findByUserIdAndRole(String userId, RoleEntity role);

    void deleteByUserIdAndRole(String userId, RoleEntity role);

    @Query("SELECT COUNT(ur) FROM UserRole ur WHERE ur.role.roleKey = :roleKey")
    long countByRoleKey(@Param("roleKey") String roleKey);
}
