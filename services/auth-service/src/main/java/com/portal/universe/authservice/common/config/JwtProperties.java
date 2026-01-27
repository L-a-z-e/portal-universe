package com.portal.universe.authservice.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * JWT 설정을 관리하는 Properties 클래스입니다.
 * 키 교체(Key Rotation)를 지원하기 위해 여러 개의 키를 관리할 수 있습니다.
 *
 * <p>토큰 생성 시에는 currentKeyId에 해당하는 키를 사용하며,
 * 토큰 검증 시에는 JWT 헤더의 kid(Key ID)를 확인하여 적절한 키를 선택합니다.</p>
 *
 * <p>키 교체 절차:</p>
 * <ol>
 *   <li>새 키를 keys 맵에 추가</li>
 *   <li>currentKeyId를 새 키로 변경</li>
 *   <li>이전 키의 expiresAt을 설정 (토큰 만료 기간 고려)</li>
 *   <li>롤링 배포</li>
 *   <li>이전 키로 발급된 토큰이 모두 만료된 후 키 제거</li>
 * </ol>
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * 현재 토큰 서명에 사용할 키 ID
     * 이 ID에 해당하는 키가 새로운 토큰 생성 시 사용됩니다.
     */
    private String currentKeyId;

    /**
     * 여러 개의 JWT 서명 키를 관리하는 맵
     * Key: 키 ID (예: "key-2026-01")
     * Value: KeyConfig 객체
     */
    private Map<String, KeyConfig> keys;

    /**
     * Access Token 만료 시간 (밀리초)
     * 기본값: 900000ms (15분)
     */
    private long accessTokenExpiration;

    /**
     * Refresh Token 만료 시간 (밀리초)
     * 기본값: 604800000ms (7일)
     */
    private long refreshTokenExpiration;

    /**
     * 개별 JWT 서명 키 설정을 담는 내부 클래스입니다.
     */
    @Getter
    @Setter
    public static class KeyConfig {

        /**
         * JWT 서명에 사용되는 비밀키
         * 최소 256비트(32바이트) 이상이어야 합니다.
         */
        private String secretKey;

        /**
         * 키가 활성화된 시점
         * 이 시점 이후로 발급된 토큰에 대해 이 키가 사용됩니다.
         */
        private LocalDateTime activatedAt;

        /**
         * 키의 만료 시점 (선택 사항)
         * null이면 만료되지 않습니다.
         * 이 시점 이후에는 토큰 검증에만 사용되고, 새로운 토큰 생성에는 사용되지 않습니다.
         */
        private LocalDateTime expiresAt;

        /**
         * 키가 만료되었는지 확인합니다.
         *
         * @return 만료되었으면 true, 아니면 false
         */
        public boolean isExpired() {
            return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
        }

        /**
         * 키가 활성화 상태인지 확인합니다.
         *
         * @return 활성화 상태이면 true, 아니면 false
         */
        public boolean isActive() {
            return activatedAt != null &&
                   LocalDateTime.now().isAfter(activatedAt) &&
                   !isExpired();
        }
    }
}
