package com.portal.universe.authservice.auth.repository;

import com.portal.universe.authservice.auth.domain.RoleDefaultMembership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoleDefaultMembershipRepository extends JpaRepository<RoleDefaultMembership, Long> {

    List<RoleDefaultMembership> findByRoleKey(String roleKey);

    boolean existsByRoleKeyAndMembershipGroup(String roleKey, String membershipGroup);

    void deleteByRoleKeyAndMembershipGroup(String roleKey, String membershipGroup);
}
