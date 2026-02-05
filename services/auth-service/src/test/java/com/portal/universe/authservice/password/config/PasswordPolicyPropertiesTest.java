package com.portal.universe.authservice.password.config;

import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PasswordPolicyProperties Test")
class PasswordPolicyPropertiesTest {

    private PasswordPolicyProperties properties;

    @BeforeEach
    void setUp() {
        properties = new PasswordPolicyProperties();
    }

    @Test
    @DisplayName("기본 minLength는 8이다")
    void should_haveDefaultMinLength8() {
        assertThat(properties.getMinLength()).isEqualTo(8);
    }

    @Test
    @DisplayName("기본 maxLength는 128이다")
    void should_haveDefaultMaxLength128() {
        assertThat(properties.getMaxLength()).isEqualTo(128);
    }

    @Test
    @DisplayName("기본적으로 모든 복잡도 조건이 활성화되어 있다")
    void should_requireAllComplexityByDefault() {
        assertThat(properties.isRequireUppercase()).isTrue();
        assertThat(properties.isRequireLowercase()).isTrue();
        assertThat(properties.isRequireDigit()).isTrue();
        assertThat(properties.isRequireSpecialChar()).isTrue();
    }

    @Test
    @DisplayName("기본 historyCount는 5이다")
    void should_haveDefaultHistoryCount5() {
        assertThat(properties.getHistoryCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("기본 maxAge는 90일이다")
    void should_haveDefaultMaxAge90() {
        assertThat(properties.getMaxAge()).isEqualTo(90);
    }

    @Test
    @DisplayName("기본적으로 연속 문자 및 사용자 정보 포함이 금지되어 있다")
    void should_preventSequentialAndUserInfoByDefault() {
        assertThat(properties.isPreventSequential()).isTrue();
        assertThat(properties.isPreventUserInfo()).isTrue();
    }

    @Test
    @DisplayName("specialChars 기본값에 일반적인 특수문자가 포함되어 있다")
    void should_haveDefaultSpecialChars() {
        assertThat(properties.getSpecialChars()).contains("!", "@", "#", "$");
    }
}
