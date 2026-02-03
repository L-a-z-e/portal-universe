package com.portal.universe.notificationservice.repository;

import com.portal.universe.notificationservice.domain.Notification;
import com.portal.universe.notificationservice.domain.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    Page<Notification> findByUserIdAndStatusOrderByCreatedAtDesc(String userId, NotificationStatus status, Pageable pageable);

    long countByUserIdAndStatus(String userId, NotificationStatus status);

    Optional<Notification> findByIdAndUserId(Long id, String userId);

    @Modifying
    @Query("UPDATE Notification n SET n.status = :status, n.readAt = :readAt WHERE n.userId = :userId AND n.status = 'UNREAD'")
    int markAllAsRead(@Param("userId") String userId, @Param("status") NotificationStatus status, @Param("readAt") LocalDateTime readAt);

    void deleteByUserIdAndId(String userId, Long id);

    boolean existsByReferenceIdAndReferenceTypeAndUserId(String referenceId, String referenceType, String userId);

    Optional<Notification> findByReferenceIdAndReferenceTypeAndUserId(String referenceId, String referenceType, String userId);
}
