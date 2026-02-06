package com.portal.universe.authservice.common.util;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.*;

@DisplayName("TokenUtils Test")
class TokenUtilsTest {

    @Nested
    @DisplayName("extractBearerToken")
    class ExtractBearerToken {

        @Test
        @DisplayName("유효한 Bearer 헤더에서 토큰을 추출한다")
        void should_extractToken_when_validBearerHeader() {
            String result = TokenUtils.extractBearerToken("Bearer eyJhbGciOiJIUzI1NiJ9");

            assertThat(result).isEqualTo("eyJhbGciOiJIUzI1NiJ9");
        }

        @Test
        @DisplayName("null 헤더이면 CustomBusinessException을 던진다")
        void should_throwException_when_nullHeader() {
            assertThatThrownBy(() -> TokenUtils.extractBearerToken(null))
                    .isInstanceOf(CustomBusinessException.class);
        }

        @Test
        @DisplayName("Bearer가 아닌 prefix이면 CustomBusinessException을 던진다")
        void should_throwException_when_invalidPrefix() {
            assertThatThrownBy(() -> TokenUtils.extractBearerToken("Basic abc123"))
                    .isInstanceOf(CustomBusinessException.class);
        }

        @Test
        @DisplayName("빈 문자열이면 CustomBusinessException을 던진다")
        void should_throwException_when_emptyString() {
            assertThatThrownBy(() -> TokenUtils.extractBearerToken(""))
                    .isInstanceOf(CustomBusinessException.class);
        }
    }

    @Nested
    @DisplayName("isBearerToken")
    class IsBearerToken {

        @Test
        @DisplayName("Bearer 형식이면 true를 반환한다")
        void should_returnTrue_when_bearerToken() {
            assertThat(TokenUtils.isBearerToken("Bearer token123")).isTrue();
        }

        @Test
        @DisplayName("Bearer 형식이 아니면 false를 반환한다")
        void should_returnFalse_when_notBearerToken() {
            assertThat(TokenUtils.isBearerToken("Basic token123")).isFalse();
        }

        @Test
        @DisplayName("null이면 false를 반환한다")
        void should_returnFalse_when_null() {
            assertThat(TokenUtils.isBearerToken(null)).isFalse();
        }
    }
}
