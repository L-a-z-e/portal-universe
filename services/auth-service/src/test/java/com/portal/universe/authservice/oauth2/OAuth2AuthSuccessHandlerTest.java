package com.portal.universe.authservice.oauth2;

import com.portal.universe.authservice.auth.service.RefreshTokenService;
import com.portal.universe.authservice.auth.service.TokenService;
import com.portal.universe.authservice.common.config.JwtProperties;
import com.portal.universe.authservice.common.util.RefreshTokenCookieHelper;
import com.portal.universe.authservice.user.domain.User;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OAuth2AuthenticationSuccessHandler Test")
class OAuth2AuthSuccessHandlerTest {

    @Mock
    private TokenService tokenService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private RefreshTokenCookieHelper cookieHelper;

    @InjectMocks
    private OAuth2AuthenticationSuccessHandler handler;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    private static final String USER_UUID = "test-uuid-123";
    private static final String USER_EMAIL = "test@gmail.com";
    private static final String ACCESS_TOKEN = "mock.access.token";
    private static final String REFRESH_TOKEN = "mock.refresh.token";
    private static final String FRONTEND_BASE_URL = "http://localhost:30000";

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        ReflectionTestUtils.setField(handler, "frontendBaseUrl", FRONTEND_BASE_URL);
    }

    @Test
    @DisplayName("OAuth2 인증 성공 시 JWT 토큰을 발급하고 프론트엔드로 리다이렉트한다")
    void should_issueTokensAndRedirect_when_authenticationSuccess() throws IOException, ServletException {
        // given
        User user = createUserWithUuid(USER_EMAIL, USER_UUID);
        CustomOAuth2User oAuth2User = new CustomOAuth2User(
                user, Map.of("sub", "google-id"), "sub", List.of("ROLE_USER"));

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(oAuth2User);

        when(tokenService.generateAccessToken(user)).thenReturn(ACCESS_TOKEN);
        when(tokenService.generateRefreshToken(user)).thenReturn(REFRESH_TOKEN);
        when(jwtProperties.getAccessTokenExpiration()).thenReturn(900000L); // 15분

        // when
        handler.onAuthenticationSuccess(request, response, authentication);

        // then
        verify(tokenService).generateAccessToken(user);
        verify(tokenService).generateRefreshToken(user);
        verify(refreshTokenService).saveRefreshToken(USER_UUID, REFRESH_TOKEN);
        verify(cookieHelper).setCookie(response, REFRESH_TOKEN);

        assertThat(response.getRedirectedUrl()).isNotNull();
        assertThat(response.getRedirectedUrl()).startsWith(FRONTEND_BASE_URL + "/oauth2/callback#");
        assertThat(response.getRedirectedUrl()).contains("access_token=" + ACCESS_TOKEN);
        assertThat(response.getRedirectedUrl()).contains("expires_in=900");
    }

    @Test
    @DisplayName("응답이 이미 커밋되었으면 처리를 중단한다")
    void should_doNothing_when_responseIsAlreadyCommitted() throws IOException, ServletException {
        // given
        MockHttpServletResponse committedResponse = mock(MockHttpServletResponse.class);
        when(committedResponse.isCommitted()).thenReturn(true);

        Authentication authentication = mock(Authentication.class);

        // when
        handler.onAuthenticationSuccess(request, committedResponse, authentication);

        // then
        verifyNoInteractions(tokenService, refreshTokenService, cookieHelper);
    }

    @Test
    @DisplayName("expiresIn은 밀리초를 초 단위로 변환한다")
    void should_convertExpiresInToSeconds_when_redirecting() throws IOException, ServletException {
        // given
        User user = createUserWithUuid(USER_EMAIL, USER_UUID);
        CustomOAuth2User oAuth2User = new CustomOAuth2User(
                user, Map.of("sub", "google-id"), "sub", List.of("ROLE_USER"));

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(oAuth2User);

        when(tokenService.generateAccessToken(user)).thenReturn(ACCESS_TOKEN);
        when(tokenService.generateRefreshToken(user)).thenReturn(REFRESH_TOKEN);
        when(jwtProperties.getAccessTokenExpiration()).thenReturn(1800000L); // 30분

        // when
        handler.onAuthenticationSuccess(request, response, authentication);

        // then
        assertThat(response.getRedirectedUrl()).contains("expires_in=1800");
    }

    // ========== Helper Methods ==========

    private User createUserWithUuid(String email, String uuid) {
        User user = new User(email, null);
        try {
            Field uuidField = User.class.getDeclaredField("uuid");
            uuidField.setAccessible(true);
            uuidField.set(user, uuid);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return user;
    }
}
