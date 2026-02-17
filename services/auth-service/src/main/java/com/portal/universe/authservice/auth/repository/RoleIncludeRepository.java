package com.portal.universe.authservice.auth.repository;

import com.portal.universe.authservice.auth.domain.RoleEntity;
import com.portal.universe.authservice.auth.domain.RoleInclude;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoleIncludeRepository extends JpaRepository<RoleInclude, Long> {

    @Query("SELECT ri FROM RoleInclude ri JOIN FETCH ri.includedRole WHERE ri.role = :role")
    List<RoleInclude> findByRole(@Param("role") RoleEntity role);

    @Query("SELECT ri FROM RoleInclude ri JOIN FETCH ri.includedRole WHERE ri.role.roleKey = :roleKey")
    List<RoleInclude> findByRoleRoleKey(@Param("roleKey") String roleKey);

    boolean existsByRoleAndIncludedRole(RoleEntity role, RoleEntity includedRole);

    void deleteByRoleAndIncludedRole(RoleEntity role, RoleEntity includedRole);

    @Query("SELECT ri FROM RoleInclude ri JOIN FETCH ri.role JOIN FETCH ri.includedRole")
    List<RoleInclude> findAllWithRoles();
}
