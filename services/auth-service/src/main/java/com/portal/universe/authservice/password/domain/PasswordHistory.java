package com.portal.universe.authservice.password.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 사용자의 비밀번호 변경 이력을 저장하는 엔티티입니다.
 * 최근 N개의 비밀번호 재사용을 방지하기 위해 사용됩니다.
 */
@Entity
@Table(name = "password_history",
        indexes = {
                @Index(name = "idx_user_id_created_at", columnList = "user_id,created_at")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class PasswordHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 255)
    private String passwordHash;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public PasswordHistory(Long userId, String passwordHash) {
        this.userId = userId;
        this.passwordHash = passwordHash;
    }

    /**
     * 정적 팩토리 메서드
     */
    public static PasswordHistory create(Long userId, String passwordHash) {
        return new PasswordHistory(userId, passwordHash);
    }
}
