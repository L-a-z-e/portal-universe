package com.portal.universe.authservice.oauth2;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("OAuth2AuthenticationFailureHandler Test")
class OAuth2AuthFailureHandlerTest {

    @InjectMocks
    private OAuth2AuthenticationFailureHandler handler;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Nested
    @DisplayName("onAuthenticationFailure")
    class OnAuthenticationFailure {

        @Test
        @DisplayName("issuerUri가 설정되었으면 해당 URL 기반으로 에러와 함께 리다이렉트한다")
        void should_redirectWithError_when_issuerUriIsConfigured() throws IOException, ServletException {
            // given
            String issuerUri = "https://auth.portal-universe.com";
            ReflectionTestUtils.setField(handler, "issuerUri", issuerUri);

            AuthenticationException exception = new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_token"),
                    "Invalid credentials"
            );

            // when
            handler.onAuthenticationFailure(request, response, exception);

            // then
            String redirectedUrl = response.getRedirectedUrl();
            assertThat(redirectedUrl).isNotNull();
            assertThat(redirectedUrl).startsWith(issuerUri + "/login");
            assertThat(redirectedUrl).contains("error=oauth2_error");
            assertThat(redirectedUrl).contains("message=");
        }

        @Test
        @DisplayName("issuerUri가 빈 문자열이면 상대 경로로 리다이렉트한다")
        void should_redirectToRelativePath_when_issuerUriIsEmpty() throws IOException, ServletException {
            // given
            ReflectionTestUtils.setField(handler, "issuerUri", "");

            AuthenticationException exception = new OAuth2AuthenticationException(
                    new OAuth2Error("access_denied"),
                    "Access denied"
            );

            // when
            handler.onAuthenticationFailure(request, response, exception);

            // then
            String redirectedUrl = response.getRedirectedUrl();
            assertThat(redirectedUrl).isNotNull();
            assertThat(redirectedUrl).startsWith("/login");
            assertThat(redirectedUrl).contains("error=oauth2_error");
        }

        @Test
        @DisplayName("issuerUri가 null이면 상대 경로로 리다이렉트한다")
        void should_redirectToRelativePath_when_issuerUriIsNull() throws IOException, ServletException {
            // given
            ReflectionTestUtils.setField(handler, "issuerUri", null);

            AuthenticationException exception = new OAuth2AuthenticationException(
                    new OAuth2Error("server_error"),
                    "Internal error"
            );

            // when
            handler.onAuthenticationFailure(request, response, exception);

            // then
            String redirectedUrl = response.getRedirectedUrl();
            assertThat(redirectedUrl).isNotNull();
            assertThat(redirectedUrl).startsWith("/login");
            assertThat(redirectedUrl).contains("error=oauth2_error");
        }

        @Test
        @DisplayName("예외 메시지가 redirect URL의 message 파라미터에 포함된다")
        void should_includeExceptionMessage_when_redirecting() throws IOException, ServletException {
            // given
            ReflectionTestUtils.setField(handler, "issuerUri", "");

            String errorMessage = "User not found";
            AuthenticationException exception = new OAuth2AuthenticationException(
                    new OAuth2Error("user_not_found"),
                    errorMessage
            );

            // when
            handler.onAuthenticationFailure(request, response, exception);

            // then
            String redirectedUrl = response.getRedirectedUrl();
            assertThat(redirectedUrl).isNotNull();
            assertThat(redirectedUrl).contains("message=");
        }
    }
}
