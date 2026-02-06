package com.portal.universe.apigateway.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtProperties Test")
class JwtPropertiesTest {

    @Nested
    @DisplayName("KeyConfig.isExpired()")
    class IsExpired {

        @Test
        @DisplayName("만료 시간이 과거이면 true를 반환한다")
        void should_returnTrue_when_keyExpired() {
            var keyConfig = new JwtProperties.KeyConfig();
            keyConfig.setExpiresAt(LocalDateTime.now().minusHours(1));

            assertThat(keyConfig.isExpired()).isTrue();
        }

        @Test
        @DisplayName("만료 시간이 미래이면 false를 반환한다")
        void should_returnFalse_when_keyNotExpired() {
            var keyConfig = new JwtProperties.KeyConfig();
            keyConfig.setExpiresAt(LocalDateTime.now().plusHours(1));

            assertThat(keyConfig.isExpired()).isFalse();
        }

        @Test
        @DisplayName("만료 시간이 null이면 false를 반환한다")
        void should_returnFalse_when_expiresAtIsNull() {
            var keyConfig = new JwtProperties.KeyConfig();
            keyConfig.setExpiresAt(null);

            assertThat(keyConfig.isExpired()).isFalse();
        }
    }

    @Nested
    @DisplayName("KeyConfig.isActive()")
    class IsActive {

        @Test
        @DisplayName("활성화 시간이 과거이고 만료 전이면 true를 반환한다")
        void should_returnTrue_when_keyIsActive() {
            var keyConfig = new JwtProperties.KeyConfig();
            keyConfig.setActivatedAt(LocalDateTime.now().minusHours(1));
            keyConfig.setExpiresAt(LocalDateTime.now().plusHours(1));

            assertThat(keyConfig.isActive()).isTrue();
        }

        @Test
        @DisplayName("활성화 시간이 미래이면 false를 반환한다")
        void should_returnFalse_when_keyNotYetActivated() {
            var keyConfig = new JwtProperties.KeyConfig();
            keyConfig.setActivatedAt(LocalDateTime.now().plusHours(1));
            keyConfig.setExpiresAt(LocalDateTime.now().plusHours(2));

            assertThat(keyConfig.isActive()).isFalse();
        }

        @Test
        @DisplayName("activatedAt이 null이면 false를 반환한다")
        void should_returnFalse_when_activatedAtIsNull() {
            var keyConfig = new JwtProperties.KeyConfig();
            keyConfig.setActivatedAt(null);

            assertThat(keyConfig.isActive()).isFalse();
        }

        @Test
        @DisplayName("활성화되었지만 만료되었으면 false를 반환한다")
        void should_returnFalse_when_keyExpiredButWasActive() {
            var keyConfig = new JwtProperties.KeyConfig();
            keyConfig.setActivatedAt(LocalDateTime.now().minusHours(2));
            keyConfig.setExpiresAt(LocalDateTime.now().minusHours(1));

            assertThat(keyConfig.isActive()).isFalse();
        }

        @Test
        @DisplayName("expiresAt이 null이면 만료되지 않은 것으로 취급한다")
        void should_returnTrue_when_activeAndNoExpiry() {
            var keyConfig = new JwtProperties.KeyConfig();
            keyConfig.setActivatedAt(LocalDateTime.now().minusHours(1));
            keyConfig.setExpiresAt(null);

            assertThat(keyConfig.isActive()).isTrue();
        }
    }

    @Test
    @DisplayName("currentKeyId getter/setter가 정상 동작한다")
    void should_setAndGetCurrentKeyId() {
        var props = new JwtProperties();
        props.setCurrentKeyId("key-2026-01");

        assertThat(props.getCurrentKeyId()).isEqualTo("key-2026-01");
    }

    @Test
    @DisplayName("keys Map getter/setter가 정상 동작한다")
    void should_setAndGetKeys() {
        var props = new JwtProperties();
        var keyConfig = new JwtProperties.KeyConfig();
        keyConfig.setSecretKey("test-secret");
        props.setKeys(Map.of("key1", keyConfig));

        assertThat(props.getKeys()).containsKey("key1");
        assertThat(props.getKeys().get("key1").getSecretKey()).isEqualTo("test-secret");
    }
}
