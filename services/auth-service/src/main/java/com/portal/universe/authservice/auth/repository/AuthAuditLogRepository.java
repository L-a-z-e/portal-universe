package com.portal.universe.authservice.auth.repository;

import com.portal.universe.authservice.auth.domain.AuditEventType;
import com.portal.universe.authservice.auth.domain.AuthAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuthAuditLogRepository extends JpaRepository<AuthAuditLog, Long> {

    Page<AuthAuditLog> findByTargetUserId(String targetUserId, Pageable pageable);

    Page<AuthAuditLog> findByEventType(AuditEventType eventType, Pageable pageable);

    Page<AuthAuditLog> findByTargetUserIdAndEventType(String targetUserId, AuditEventType eventType, Pageable pageable);

    List<AuthAuditLog> findTop5ByOrderByCreatedAtDesc();

    Page<AuthAuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
