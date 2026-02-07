package com.portal.universe.authservice.auth.repository;

import com.portal.universe.authservice.auth.domain.MembershipStatus;
import com.portal.universe.authservice.auth.domain.UserMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserMembershipRepository extends JpaRepository<UserMembership, Long> {

    List<UserMembership> findByUserId(String userId);

    Optional<UserMembership> findByUserIdAndMembershipGroup(String userId, String membershipGroup);

    List<UserMembership> findByUserIdAndStatus(String userId, MembershipStatus status);

    @Query("SELECT um FROM UserMembership um JOIN FETCH um.tier t WHERE um.userId = :userId AND um.status = 'ACTIVE' AND t.active = true")
    List<UserMembership> findActiveByUserId(@Param("userId") String userId);

    boolean existsByUserIdAndMembershipGroup(String userId, String membershipGroup);

    long countByStatus(MembershipStatus status);

    @Query("SELECT um.membershipGroup, um.tier.tierKey, um.tier.displayName, COUNT(um) " +
            "FROM UserMembership um WHERE um.status = 'ACTIVE' " +
            "GROUP BY um.membershipGroup, um.tier.tierKey, um.tier.displayName " +
            "ORDER BY um.membershipGroup, um.tier.displayName")
    List<Object[]> countActiveGroupByGroupAndTier();
}
