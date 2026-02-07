package com.portal.universe.authservice.auth.repository;

import com.portal.universe.authservice.auth.domain.MembershipTier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MembershipTierRepository extends JpaRepository<MembershipTier, Long> {

    List<MembershipTier> findByMembershipGroupOrderBySortOrder(String membershipGroup);

    Optional<MembershipTier> findByMembershipGroupAndTierKey(String membershipGroup, String tierKey);

    boolean existsByMembershipGroupAndTierKey(String membershipGroup, String tierKey);

    List<MembershipTier> findByActiveTrue();
}
