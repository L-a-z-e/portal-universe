package com.portal.universe.authservice.auth.service;

import com.portal.universe.authservice.auth.domain.*;
import com.portal.universe.authservice.auth.dto.seller.*;
import com.portal.universe.authservice.auth.repository.*;
import com.portal.universe.authservice.common.exception.AuthErrorCode;
import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.event.auth.RoleAssignedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 셀러 신청/승인 워크플로우를 담당합니다.
 * 사용자가 셀러 신청을 하면 관리자가 승인/거절하고,
 * 승인 시 ROLE_SHOPPING_SELLER 역할을 자동 할당합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerApplicationService {

    private final SellerApplicationRepository sellerApplicationRepository;
    private final RoleEntityRepository roleEntityRepository;
    private final UserRoleRepository userRoleRepository;
    private final AuthAuditLogRepository auditLogRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 셀러 신청서를 제출합니다.
     */
    @Transactional
    public SellerApplicationResponse apply(String userId, SellerApplicationRequest request) {
        // 이미 PENDING 상태인 신청이 있는지 확인
        if (sellerApplicationRepository.existsByUserIdAndStatus(userId, SellerApplicationStatus.PENDING)) {
            throw new CustomBusinessException(AuthErrorCode.SELLER_APPLICATION_ALREADY_PENDING);
        }

        // 이미 SELLER 역할이 있는지 확인
        boolean alreadySeller = userRoleRepository.findActiveRoleKeysByUserId(userId)
                .contains("ROLE_SHOPPING_SELLER");
        if (alreadySeller) {
            throw new CustomBusinessException(AuthErrorCode.ROLE_ALREADY_ASSIGNED);
        }

        SellerApplication application = SellerApplication.builder()
                .userId(userId)
                .businessName(request.businessName())
                .businessNumber(request.businessNumber())
                .reason(request.reason())
                .build();

        SellerApplication saved = sellerApplicationRepository.save(application);

        logAudit(AuditEventType.SELLER_APPLICATION_SUBMITTED, userId, userId,
                "Seller application submitted: " + request.businessName());

        log.info("Seller application submitted: userId={}, businessName={}", userId, request.businessName());
        return SellerApplicationResponse.from(saved);
    }

    /**
     * 내 셀러 신청 현황을 조회합니다.
     */
    public SellerApplicationResponse getMyApplication(String userId) {
        SellerApplication application = sellerApplicationRepository
                .findByUserIdAndStatus(userId, SellerApplicationStatus.PENDING)
                .orElseThrow(() -> new CustomBusinessException(AuthErrorCode.SELLER_APPLICATION_NOT_FOUND));
        return SellerApplicationResponse.from(application);
    }

    /**
     * 대기 중인 셀러 신청 목록을 조회합니다 (관리자용).
     */
    public Page<SellerApplicationResponse> getPendingApplications(Pageable pageable) {
        return sellerApplicationRepository
                .findByStatus(SellerApplicationStatus.PENDING, pageable)
                .map(SellerApplicationResponse::from);
    }

    /**
     * 모든 셀러 신청 목록을 조회합니다 (관리자용).
     */
    public Page<SellerApplicationResponse> getAllApplications(Pageable pageable) {
        return sellerApplicationRepository
                .findAllByOrderByCreatedAtDesc(pageable)
                .map(SellerApplicationResponse::from);
    }

    /**
     * 셀러 신청을 심사합니다 (승인 또는 거절).
     * 승인 시 ROLE_SHOPPING_SELLER 역할을 자동 할당합니다.
     */
    @Transactional
    public SellerApplicationResponse review(Long applicationId, SellerApplicationReviewRequest request, String reviewerId) {
        SellerApplication application = sellerApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new CustomBusinessException(AuthErrorCode.SELLER_APPLICATION_NOT_FOUND));

        if (!application.isPending()) {
            throw new CustomBusinessException(AuthErrorCode.SELLER_APPLICATION_ALREADY_PROCESSED);
        }

        if (Boolean.TRUE.equals(request.approved())) {
            application.approve(reviewerId, request.reviewComment());

            // ROLE_SHOPPING_SELLER 자동 할당 + seller:shopping BRONZE 멤버십 생성 (원자적)
            assignSellerRoleAndMembership(application.getUserId(), reviewerId);

            logAudit(AuditEventType.SELLER_APPLICATION_APPROVED, reviewerId, application.getUserId(),
                    "Seller application approved: " + application.getBusinessName());

            log.info("Seller application approved: applicationId={}, userId={}, by={}",
                    applicationId, application.getUserId(), reviewerId);
        } else {
            application.reject(reviewerId, request.reviewComment());

            logAudit(AuditEventType.SELLER_APPLICATION_REJECTED, reviewerId, application.getUserId(),
                    "Seller application rejected: " + request.reviewComment());

            log.info("Seller application rejected: applicationId={}, userId={}, by={}, reason={}",
                    applicationId, application.getUserId(), reviewerId, request.reviewComment());
        }

        return SellerApplicationResponse.from(application);
    }

    private void assignSellerRoleAndMembership(String userId, String assignedBy) {
        RoleEntity sellerRole = roleEntityRepository.findByRoleKey("ROLE_SHOPPING_SELLER")
                .orElseThrow(() -> new CustomBusinessException(AuthErrorCode.ROLE_NOT_FOUND));

        boolean alreadyAssigned = userRoleRepository.findByUserIdWithRole(userId).stream()
                .anyMatch(ur -> "ROLE_SHOPPING_SELLER".equals(ur.getRole().getRoleKey()) && !ur.isExpired());
        if (!alreadyAssigned) {
            userRoleRepository.save(UserRole.builder()
                    .userId(userId)
                    .role(sellerRole)
                    .assignedBy(assignedBy)
                    .build());

            logAudit(AuditEventType.ROLE_ASSIGNED, assignedBy, userId,
                    "ROLE_SHOPPING_SELLER auto-assigned on application approval");

            // 역할 할당 이벤트 → MembershipAutoAssignHandler가 seller:shopping/BRONZE 자동 생성
            eventPublisher.publishEvent(RoleAssignedEvent.of(userId, "ROLE_SHOPPING_SELLER", assignedBy));
        }
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
