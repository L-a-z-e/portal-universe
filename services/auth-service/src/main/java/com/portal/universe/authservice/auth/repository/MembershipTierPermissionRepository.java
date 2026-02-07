package com.portal.universe.authservice.auth.repository;

import com.portal.universe.authservice.auth.domain.MembershipTier;
import com.portal.universe.authservice.auth.domain.MembershipTierPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MembershipTierPermissionRepository extends JpaRepository<MembershipTierPermission, Long> {

    List<MembershipTierPermission> findByTier(MembershipTier tier);

    @Query("SELECT mtp.permission.permissionKey FROM MembershipTierPermission mtp " +
            "WHERE mtp.tier.membershipGroup = :membershipGroup AND mtp.tier.tierKey = :tierKey")
    List<String> findPermissionKeysByGroupAndTier(@Param("membershipGroup") String membershipGroup,
                                                   @Param("tierKey") String tierKey);
}
