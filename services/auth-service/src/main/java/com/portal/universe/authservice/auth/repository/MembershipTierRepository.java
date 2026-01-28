package com.portal.universe.authservice.auth.repository;

import com.portal.universe.authservice.auth.domain.MembershipTier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MembershipTierRepository extends JpaRepository<MembershipTier, Long> {

    List<MembershipTier> findByServiceNameOrderBySortOrder(String serviceName);

    Optional<MembershipTier> findByServiceNameAndTierKey(String serviceName, String tierKey);

    boolean existsByServiceNameAndTierKey(String serviceName, String tierKey);

    List<MembershipTier> findByActiveTrue();
}
