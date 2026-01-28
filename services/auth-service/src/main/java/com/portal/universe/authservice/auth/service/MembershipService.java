package com.portal.universe.authservice.auth.service;

import com.portal.universe.authservice.auth.domain.*;
import com.portal.universe.authservice.auth.dto.membership.*;
import com.portal.universe.authservice.auth.repository.*;
import com.portal.universe.authservice.common.exception.AuthErrorCode;
import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 멤버십 관리 비즈니스 로직을 담당합니다.
 * 사용자 멤버십 조회, 티어 변경, 취소 등을 수행합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MembershipService {

    private final UserMembershipRepository userMembershipRepository;
    private final MembershipTierRepository membershipTierRepository;
    private final AuthAuditLogRepository auditLogRepository;

    /**
     * 사용자의 모든 멤버십을 조회합니다.
     */
    public List<MembershipResponse> getUserMemberships(String userId) {
        return userMembershipRepository.findByUserId(userId).stream()
                .map(MembershipResponse::from)
                .toList();
    }

    /**
     * 사용자의 특정 서비스 멤버십을 조회합니다.
     */
    public MembershipResponse getUserMembership(String userId, String serviceName) {
        UserMembership membership = userMembershipRepository
                .findByUserIdAndServiceName(userId, serviceName)
                .orElseThrow(() -> new CustomBusinessException(AuthErrorCode.MEMBERSHIP_NOT_FOUND));
        return MembershipResponse.from(membership);
    }

    /**
     * 서비스별 사용 가능한 멤버십 티어를 조회합니다.
     */
    public List<MembershipTierResponse> getServiceTiers(String serviceName) {
        return membershipTierRepository.findByServiceNameOrderBySortOrder(serviceName).stream()
                .map(MembershipTierResponse::from)
                .toList();
    }

    /**
     * 사용자의 멤버십 티어를 변경합니다.
     */
    @Transactional
    public MembershipResponse changeMembershipTier(String userId, ChangeMembershipRequest request) {
        MembershipTier newTier = membershipTierRepository
                .findByServiceNameAndTierKey(request.serviceName(), request.tierKey())
                .orElseThrow(() -> new CustomBusinessException(AuthErrorCode.MEMBERSHIP_TIER_NOT_FOUND));

        UserMembership membership = userMembershipRepository
                .findByUserIdAndServiceName(userId, request.serviceName())
                .orElseThrow(() -> new CustomBusinessException(AuthErrorCode.MEMBERSHIP_NOT_FOUND));

        String oldTier = membership.getTier().getTierKey();
        membership.changeTier(newTier);

        logAudit(AuditEventType.MEMBERSHIP_UPGRADED, userId, userId,
                String.format("Membership changed: %s %s -> %s", request.serviceName(), oldTier, request.tierKey()));

        log.info("Membership tier changed: userId={}, service={}, {} -> {}",
                userId, request.serviceName(), oldTier, request.tierKey());
        return MembershipResponse.from(membership);
    }

    /**
     * 관리자가 사용자의 멤버십 티어를 변경합니다.
     */
    @Transactional
    public MembershipResponse adminChangeMembershipTier(String userId, ChangeMembershipRequest request, String adminId) {
        MembershipTier newTier = membershipTierRepository
                .findByServiceNameAndTierKey(request.serviceName(), request.tierKey())
                .orElseThrow(() -> new CustomBusinessException(AuthErrorCode.MEMBERSHIP_TIER_NOT_FOUND));

        UserMembership membership = userMembershipRepository
                .findByUserIdAndServiceName(userId, request.serviceName())
                .orElseGet(() -> createDefaultMembership(userId, request.serviceName(), newTier));

        String oldTier = membership.getTier().getTierKey();
        membership.changeTier(newTier);

        logAudit(AuditEventType.MEMBERSHIP_UPGRADED, adminId, userId,
                String.format("Admin changed membership: %s %s -> %s", request.serviceName(), oldTier, request.tierKey()));

        log.info("Admin changed membership: userId={}, service={}, {} -> {}, by={}",
                userId, request.serviceName(), oldTier, request.tierKey(), adminId);
        return MembershipResponse.from(membership);
    }

    /**
     * 사용자의 멤버십을 취소합니다.
     */
    @Transactional
    public void cancelMembership(String userId, String serviceName) {
        UserMembership membership = userMembershipRepository
                .findByUserIdAndServiceName(userId, serviceName)
                .orElseThrow(() -> new CustomBusinessException(AuthErrorCode.MEMBERSHIP_NOT_FOUND));

        if (!membership.isActive()) {
            throw new CustomBusinessException(AuthErrorCode.MEMBERSHIP_EXPIRED);
        }

        // FREE 티어로 다운그레이드 (취소 = FREE로 복귀)
        MembershipTier freeTier = membershipTierRepository
                .findByServiceNameAndTierKey(serviceName, "FREE")
                .orElseThrow(() -> new CustomBusinessException(AuthErrorCode.MEMBERSHIP_TIER_NOT_FOUND));

        String oldTier = membership.getTier().getTierKey();
        membership.changeTier(freeTier);
        membership.cancel();

        logAudit(AuditEventType.MEMBERSHIP_CANCELLED, userId, userId,
                String.format("Membership cancelled: %s (was %s)", serviceName, oldTier));

        log.info("Membership cancelled: userId={}, service={}, was={}", userId, serviceName, oldTier);
    }

    private UserMembership createDefaultMembership(String userId, String serviceName, MembershipTier tier) {
        UserMembership membership = UserMembership.builder()
                .userId(userId)
                .serviceName(serviceName)
                .tier(tier)
                .build();
        return userMembershipRepository.save(membership);
    }

    private void logAudit(AuditEventType eventType, String actorId, String targetUserId, String details) {
        AuthAuditLog auditLog = AuthAuditLog.builder()
                .eventType(eventType)
                .actorUserId(actorId)
                .targetUserId(targetUserId)
                .details(details)
                .build();
        auditLogRepository.save(auditLog);
    }
}
