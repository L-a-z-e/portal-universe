package com.portal.universe.authservice.common.config;

import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtProperties Test")
class JwtPropertiesTest {

    private JwtProperties jwtProperties;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
    }

    @Nested
    @DisplayName("KeyConfig")
    class KeyConfigTest {

        @Test
        @DisplayName("expiresAt이 과거이면 isExpired() = true")
        void should_returnTrue_when_keyIsExpired() {
            var keyConfig = new JwtProperties.KeyConfig();
            keyConfig.setExpiresAt(LocalDateTime.now().minusDays(1));

            assertThat(keyConfig.isExpired()).isTrue();
        }

        @Test
        @DisplayName("expiresAt이 미래이면 isExpired() = false")
        void should_returnFalse_when_keyIsNotExpired() {
            var keyConfig = new JwtProperties.KeyConfig();
            keyConfig.setExpiresAt(LocalDateTime.now().plusDays(1));

            assertThat(keyConfig.isExpired()).isFalse();
        }

        @Test
        @DisplayName("expiresAt이 null이면 isExpired() = false")
        void should_returnFalse_when_expiresAtIsNull() {
            var keyConfig = new JwtProperties.KeyConfig();
            keyConfig.setExpiresAt(null);

            assertThat(keyConfig.isExpired()).isFalse();
        }

        @Test
        @DisplayName("activatedAt이 과거이고 만료되지 않으면 isActive() = true")
        void should_returnTrue_when_keyIsActive() {
            var keyConfig = new JwtProperties.KeyConfig();
            keyConfig.setActivatedAt(LocalDateTime.now().minusDays(1));
            keyConfig.setExpiresAt(LocalDateTime.now().plusDays(30));

            assertThat(keyConfig.isActive()).isTrue();
        }

        @Test
        @DisplayName("activatedAt이 미래이면 isActive() = false")
        void should_returnFalse_when_keyIsNotYetActive() {
            var keyConfig = new JwtProperties.KeyConfig();
            keyConfig.setActivatedAt(LocalDateTime.now().plusDays(1));
            keyConfig.setExpiresAt(LocalDateTime.now().plusDays(30));

            assertThat(keyConfig.isActive()).isFalse();
        }

        @Test
        @DisplayName("activatedAt이 null이면 isActive() = false")
        void should_returnFalse_when_activatedAtIsNull() {
            var keyConfig = new JwtProperties.KeyConfig();
            keyConfig.setActivatedAt(null);

            assertThat(keyConfig.isActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("Properties")
    class PropertiesTest {

        @Test
        @DisplayName("currentKeyId getter/setter 검증")
        void should_returnCurrentKeyId() {
            jwtProperties.setCurrentKeyId("key-2026-01");

            assertThat(jwtProperties.getCurrentKeyId()).isEqualTo("key-2026-01");
        }

        @Test
        @DisplayName("accessTokenExpiration 값 검증")
        void should_returnAccessTokenExpiration() {
            jwtProperties.setAccessTokenExpiration(900_000L);

            assertThat(jwtProperties.getAccessTokenExpiration()).isEqualTo(900_000L);
        }

        @Test
        @DisplayName("refreshTokenExpiration 값 검증")
        void should_returnRefreshTokenExpiration() {
            jwtProperties.setRefreshTokenExpiration(604_800_000L);

            assertThat(jwtProperties.getRefreshTokenExpiration()).isEqualTo(604_800_000L);
        }

        @Test
        @DisplayName("keys Map 설정/조회 검증")
        void should_returnKeysMap() {
            var keyConfig = new JwtProperties.KeyConfig();
            keyConfig.setSecretKey("test-secret");
            jwtProperties.setKeys(Map.of("key-1", keyConfig));

            assertThat(jwtProperties.getKeys()).containsKey("key-1");
            assertThat(jwtProperties.getKeys().get("key-1").getSecretKey()).isEqualTo("test-secret");
        }
    }
}
