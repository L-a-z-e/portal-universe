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

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MembershipService {

    private final UserMembershipRepository userMembershipRepository;
    private final MembershipTierRepository membershipTierRepository;
    private final AuthAuditLogRepository auditLogRepository;

    public List<MembershipResponse> getUserMemberships(String userId) {
        return userMembershipRepository.findByUserId(userId).stream()
                .map(MembershipResponse::from)
                .toList();
    }

    public MembershipResponse getUserMembership(String userId, String membershipGroup) {
        UserMembership membership = userMembershipRepository
                .findByUserIdAndMembershipGroup(userId, membershipGroup)
                .orElseThrow(() -> new CustomBusinessException(AuthErrorCode.MEMBERSHIP_NOT_FOUND));
        return MembershipResponse.from(membership);
    }

    public List<String> getAllMembershipGroups() {
        return membershipTierRepository.findByActiveTrue().stream()
                .map(MembershipTier::getMembershipGroup)
                .distinct()
                .sorted()
                .toList();
    }

    public List<MembershipTierResponse> getGroupTiers(String membershipGroup) {
        return membershipTierRepository.findByMembershipGroupOrderBySortOrder(membershipGroup).stream()
                .map(MembershipTierResponse::from)
                .toList();
    }

    @Transactional
    public MembershipResponse changeMembershipTier(String userId, ChangeMembershipRequest request) {
        MembershipGroupConstants.validate(request.membershipGroup());

        MembershipTier newTier = membershipTierRepository
                .findByMembershipGroupAndTierKey(request.membershipGroup(), request.tierKey())
                .orElseThrow(() -> new CustomBusinessException(AuthErrorCode.MEMBERSHIP_TIER_NOT_FOUND));

        UserMembership membership = userMembershipRepository
                .findByUserIdAndMembershipGroup(userId, request.membershipGroup())
                .orElseThrow(() -> new CustomBusinessException(AuthErrorCode.MEMBERSHIP_NOT_FOUND));

        String oldTier = membership.getTier().getTierKey();
        membership.changeTier(newTier);

        logAudit(AuditEventType.MEMBERSHIP_UPGRADED, userId, userId,
                String.format("Membership changed: %s %s -> %s", request.membershipGroup(), oldTier, request.tierKey()));

        log.info("Membership tier changed: userId={}, group={}, {} -> {}",
                userId, request.membershipGroup(), oldTier, request.tierKey());
        return MembershipResponse.from(membership);
    }

    @Transactional
    public MembershipResponse adminChangeMembershipTier(String userId, ChangeMembershipRequest request, String adminId) {
        MembershipGroupConstants.validate(request.membershipGroup());

        MembershipTier newTier = membershipTierRepository
                .findByMembershipGroupAndTierKey(request.membershipGroup(), request.tierKey())
                .orElseThrow(() -> new CustomBusinessException(AuthErrorCode.MEMBERSHIP_TIER_NOT_FOUND));

        UserMembership membership = userMembershipRepository
                .findByUserIdAndMembershipGroup(userId, request.membershipGroup())
                .orElseGet(() -> createDefaultMembership(userId, request.membershipGroup(), newTier));

        String oldTier = membership.getTier().getTierKey();
        membership.changeTier(newTier);

        logAudit(AuditEventType.MEMBERSHIP_UPGRADED, adminId, userId,
                String.format("Admin changed membership: %s %s -> %s", request.membershipGroup(), oldTier, request.tierKey()));

        log.info("Admin changed membership: userId={}, group={}, {} -> {}, by={}",
                userId, request.membershipGroup(), oldTier, request.tierKey(), adminId);
        return MembershipResponse.from(membership);
    }

    @Transactional
    public void cancelMembership(String userId, String membershipGroup) {
        UserMembership membership = userMembershipRepository
                .findByUserIdAndMembershipGroup(userId, membershipGroup)
                .orElseThrow(() -> new CustomBusinessException(AuthErrorCode.MEMBERSHIP_NOT_FOUND));

        if (!membership.isActive()) {
            throw new CustomBusinessException(AuthErrorCode.MEMBERSHIP_EXPIRED);
        }

        MembershipTier freeTier = membershipTierRepository
                .findByMembershipGroupAndTierKey(membershipGroup, "FREE")
                .orElseThrow(() -> new CustomBusinessException(AuthErrorCode.MEMBERSHIP_TIER_NOT_FOUND));

        String oldTier = membership.getTier().getTierKey();
        membership.changeTier(freeTier);
        membership.cancel();

        logAudit(AuditEventType.MEMBERSHIP_CANCELLED, userId, userId,
                String.format("Membership cancelled: %s (was %s)", membershipGroup, oldTier));

        log.info("Membership cancelled: userId={}, group={}, was={}", userId, membershipGroup, oldTier);
    }

    private UserMembership createDefaultMembership(String userId, String membershipGroup, MembershipTier tier) {
        UserMembership membership = UserMembership.builder()
                .userId(userId)
                .membershipGroup(membershipGroup)
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
