package com.portal.universe.authservice.auth.event;

import com.portal.universe.authservice.auth.dto.rbac.AssignRoleRequest;
import com.portal.universe.authservice.auth.service.RbacService;
import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.event.seller.SellerApprovedEvent;
import com.portal.universe.event.seller.SellerTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SellerApprovedEventConsumer {

    private final RbacService rbacService;

    @KafkaListener(
            topics = SellerTopics.SELLER_APPROVED,
            groupId = "auth-service",
            containerFactory = "avroKafkaListenerContainerFactory"
    )
    public void onSellerApproved(SellerApprovedEvent event) {
        log.info("Received SellerApprovedEvent: userId={}, sellerId={}, approvedBy={}",
                event.getUserId(), event.getSellerId(), event.getApprovedBy());

        try {
            var request = new AssignRoleRequest(
                    event.getUserId(),
                    "ROLE_SHOPPING_SELLER",
                    null
            );
            rbacService.assignRole(request, event.getApprovedBy());
            log.info("ROLE_SHOPPING_SELLER assigned: userId={}", event.getUserId());
        } catch (CustomBusinessException e) {
            // ROLE_ALREADY_ASSIGNED — 멱등성 보장 (중복 수신 시 무시)
            log.warn("Role assignment skipped (idempotent): userId={}, reason={}",
                    event.getUserId(), e.getMessage());
        }
    }
}
