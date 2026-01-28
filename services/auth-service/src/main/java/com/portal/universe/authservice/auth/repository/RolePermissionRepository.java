package com.portal.universe.authservice.auth.repository;

import com.portal.universe.authservice.auth.domain.RoleEntity;
import com.portal.universe.authservice.auth.domain.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

    List<RolePermission> findByRole(RoleEntity role);

    @Query("SELECT rp.permission.permissionKey FROM RolePermission rp WHERE rp.role.roleKey = :roleKey")
    List<String> findPermissionKeysByRoleKey(@Param("roleKey") String roleKey);

    @Query("SELECT rp.permission.permissionKey FROM RolePermission rp WHERE rp.role.roleKey IN :roleKeys")
    List<String> findPermissionKeysByRoleKeys(@Param("roleKeys") List<String> roleKeys);

    boolean existsByRoleAndPermissionId(RoleEntity role, Long permissionId);

    void deleteByRoleAndPermissionId(RoleEntity role, Long permissionId);
}
