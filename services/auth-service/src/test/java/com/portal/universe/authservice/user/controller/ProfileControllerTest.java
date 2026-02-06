package com.portal.universe.authservice.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.universe.authservice.user.dto.profile.ChangePasswordRequest;
import com.portal.universe.authservice.user.dto.profile.DeleteAccountRequest;
import com.portal.universe.authservice.user.dto.profile.ProfileResponse;
import com.portal.universe.authservice.user.dto.profile.UpdateProfileRequest;
import com.portal.universe.authservice.user.domain.User;
import com.portal.universe.authservice.user.domain.UserProfile;
import com.portal.universe.authservice.auth.service.RefreshTokenService;
import com.portal.universe.authservice.auth.service.TokenBlacklistService;
import com.portal.universe.authservice.auth.service.TokenService;
import com.portal.universe.authservice.user.service.ProfileService;
import com.portal.universe.authservice.auth.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ProfileController Unit Test")
class ProfileControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    ProfileService profileService;

    @MockitoBean
    TokenService tokenService;

    @MockitoBean
    TokenBlacklistService tokenBlacklistService;

    @MockitoBean
    RefreshTokenService refreshTokenService;

    @MockitoBean
    JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final String USER_UUID = "test-user-uuid";
    private static final String BASE_URL = "/api/v1/profile";

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(USER_UUID, null, Collections.emptyList())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private ProfileResponse createProfileResponse() {
        return new ProfileResponse(
                USER_UUID, "user@test.com", "TestNick", "Real Name",
                "010-1234-5678", "https://img.test.com/pic.jpg",
                true, false, List.of(), LocalDateTime.now()
        );
    }

    @Nested
    @DisplayName("GET /api/v1/profile/me")
    class GetMyProfile {

        @Test
        @DisplayName("should_returnProfile_when_authenticated")
        void should_returnProfile_when_authenticated() throws Exception {
            // given
            ProfileResponse response = createProfileResponse();
            when(profileService.getProfile(USER_UUID)).thenReturn(response);

            // when & then
            mockMvc.perform(get(BASE_URL + "/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.uuid").value(USER_UUID))
                    .andExpect(jsonPath("$.data.email").value("user@test.com"))
                    .andExpect(jsonPath("$.data.nickname").value("TestNick"));
        }

        @Test
        @DisplayName("should_returnError_when_noAuthentication")
        void should_returnError_when_noAuthentication() throws Exception {
            // given
            SecurityContextHolder.clearContext();

            // when & then
            mockMvc.perform(get(BASE_URL + "/me"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/profile")
    class UpdateProfile {

        @Test
        @DisplayName("should_returnUpdatedProfileWithNewToken_when_validRequest")
        void should_returnUpdatedProfileWithNewToken_when_validRequest() throws Exception {
            // given
            UpdateProfileRequest request = new UpdateProfileRequest(
                    "UpdatedNick", "Updated Real", "010-9999-8888", "https://img.new.com/pic.jpg", true
            );

            User mockUser = mock(User.class);
            UserProfile mockProfile = mock(UserProfile.class);
            when(mockUser.getUuid()).thenReturn(USER_UUID);
            when(mockUser.getEmail()).thenReturn("user@test.com");
            when(mockUser.getProfile()).thenReturn(mockProfile);
            when(mockUser.getCreatedAt()).thenReturn(LocalDateTime.now());
            when(mockUser.getSocialAccounts()).thenReturn(Collections.emptyList());
            when(mockProfile.getNickname()).thenReturn("UpdatedNick");
            when(mockProfile.getRealName()).thenReturn("Updated Real");
            when(mockProfile.getPhoneNumber()).thenReturn("010-9999-8888");
            when(mockProfile.getProfileImageUrl()).thenReturn("https://img.new.com/pic.jpg");
            when(mockProfile.isMarketingAgree()).thenReturn(true);

            when(profileService.updateProfile(eq(USER_UUID), any(UpdateProfileRequest.class))).thenReturn(mockUser);
            when(tokenService.generateAccessToken(mockUser)).thenReturn("new-access-token");

            // when & then
            mockMvc.perform(patch(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
                    .andExpect(jsonPath("$.data.profile").exists());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/profile/password")
    class ChangePassword {

        @Test
        @DisplayName("should_returnSuccess_when_validPasswordChange")
        void should_returnSuccess_when_validPasswordChange() throws Exception {
            // given
            ChangePasswordRequest request = new ChangePasswordRequest(
                    "currentPass1!", "newPass1!", "newPass1!"
            );
            doNothing().when(profileService).changePassword(eq(USER_UUID), any(ChangePasswordRequest.class));

            // when & then
            mockMvc.perform(post(BASE_URL + "/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.message").value("비밀번호가 변경되었습니다"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/profile/account")
    class DeleteAccount {

        @Test
        @DisplayName("should_returnSuccess_when_validDeletion")
        void should_returnSuccess_when_validDeletion() throws Exception {
            // given
            DeleteAccountRequest request = new DeleteAccountRequest("password123", "Leaving");

            doNothing().when(profileService).deleteAccount(eq(USER_UUID), any(DeleteAccountRequest.class));
            when(tokenService.getRemainingExpiration("valid-access-token")).thenReturn(300000L);

            // when & then
            mockMvc.perform(delete(BASE_URL + "/account")
                            .header("Authorization", "Bearer valid-access-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.message").value("회원 탈퇴가 완료되었습니다"));

            verify(tokenBlacklistService).addToBlacklist("valid-access-token", 300000L);
            verify(refreshTokenService).deleteRefreshToken(USER_UUID);
        }
    }
}
