package com.portal.universe.authservice.common.util;

import com.portal.universe.authservice.common.config.JwtProperties;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@DisplayName("RefreshTokenCookieHelper Test")
@ExtendWith(MockitoExtension.class)
class RefreshTokenCookieHelperTest {

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private RefreshTokenCookieHelper cookieHelper;

    @Test
    @DisplayName("setCookie 호출 시 Set-Cookie 헤더에 올바른 속성을 설정한다")
    void should_setCookieWithCorrectAttributes() {
        when(jwtProperties.getRefreshTokenExpiration()).thenReturn(604_800_000L); // 7 days
        var response = new MockHttpServletResponse();

        cookieHelper.setCookie(response, "test-refresh-token");

        String setCookie = response.getHeader("Set-Cookie");
        assertThat(setCookie).isNotNull();
        assertThat(setCookie).contains("portal_refresh_token=test-refresh-token");
        assertThat(setCookie).contains("HttpOnly");
        assertThat(setCookie).contains("Path=/");
    }

    @Test
    @DisplayName("setCookie의 MaxAge는 JwtProperties.refreshTokenExpiration 기반이다")
    void should_setCookieMaxAgeFromJwtProperties() {
        when(jwtProperties.getRefreshTokenExpiration()).thenReturn(604_800_000L); // 7 days = 604800 seconds
        var response = new MockHttpServletResponse();

        cookieHelper.setCookie(response, "token");

        String setCookie = response.getHeader("Set-Cookie");
        assertThat(setCookie).contains("Max-Age=604800");
    }

    @Test
    @DisplayName("clearCookie 호출 시 MaxAge=0으로 쿠키를 삭제한다")
    void should_clearCookieWithZeroMaxAge() {
        var response = new MockHttpServletResponse();

        cookieHelper.clearCookie(response);

        String setCookie = response.getHeader("Set-Cookie");
        assertThat(setCookie).isNotNull();
        assertThat(setCookie).contains("Max-Age=0");
    }

    @Test
    @DisplayName("getCookieName()은 'portal_refresh_token'을 반환한다")
    void should_returnCorrectCookieName() {
        assertThat(RefreshTokenCookieHelper.getCookieName()).isEqualTo("portal_refresh_token");
    }
}
