package com.portal.universe.authservice.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.universe.authservice.user.dto.PasswordChangeRequest;
import com.portal.universe.authservice.user.dto.UserProfileResponse;
import com.portal.universe.authservice.user.dto.UsernameSetRequest;
import com.portal.universe.authservice.user.dto.UserProfileUpdateRequest;
import com.portal.universe.authservice.user.service.UserService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("UserController Unit Test")
class UserControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    UserService userService;

    @MockitoBean
    JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final String USER_UUID = "test-user-uuid";
    private static final String BASE_URL = "/api/v1/users";

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

    private UserProfileResponse createProfileResponse() {
        return new UserProfileResponse(
                1L, USER_UUID, "user@test.com", "TestNick", "testuser",
                "Hello", "https://img.test.com/pic.jpg", "https://test.com",
                10, 5, LocalDateTime.now()
        );
    }

    @Nested
    @DisplayName("POST /api/v1/users/signup")
    class Signup {

        @Test
        @DisplayName("should_returnSuccess_when_validSignupRequest")
        void should_returnSuccess_when_validSignupRequest() throws Exception {
            // given
            var request = new UserController.UserSignupRequest(
                    "new@test.com", "Password1!", "NewUser", "Real Name", false
            );
            when(userService.registerUser(any())).thenReturn(1L);

            // when & then
            mockMvc.perform(post(BASE_URL + "/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").value("User registered successfully"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/{username}")
    class GetProfile {

        @Test
        @DisplayName("should_returnProfile_when_usernameExists")
        void should_returnProfile_when_usernameExists() throws Exception {
            // given
            UserProfileResponse response = createProfileResponse();
            when(userService.getProfileByUsername("testuser")).thenReturn(response);

            // when & then
            mockMvc.perform(get(BASE_URL + "/testuser"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.username").value("testuser"))
                    .andExpect(jsonPath("$.data.nickname").value("TestNick"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/me")
    class GetMyProfile {

        @Test
        @DisplayName("should_returnMyProfile_when_authenticated")
        void should_returnMyProfile_when_authenticated() throws Exception {
            // given
            UserProfileResponse response = createProfileResponse();
            when(userService.getMyProfileByUuid(USER_UUID)).thenReturn(response);

            // when & then
            mockMvc.perform(get(BASE_URL + "/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.uuid").value(USER_UUID))
                    .andExpect(jsonPath("$.data.email").value("user@test.com"));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/users/me/profile")
    class UpdateProfile {

        @Test
        @DisplayName("should_returnUpdatedProfile_when_validRequest")
        void should_returnUpdatedProfile_when_validRequest() throws Exception {
            // given
            UserProfileUpdateRequest request = new UserProfileUpdateRequest(
                    "UpdatedNick", "Updated bio", "https://img.test.com/new.jpg", "https://example.com"
            );

            UserProfileResponse response = new UserProfileResponse(
                    1L, USER_UUID, "user@test.com", "UpdatedNick", "testuser",
                    "Updated bio", "https://img.test.com/new.jpg", "https://example.com",
                    10, 5, LocalDateTime.now()
            );
            when(userService.updateProfileByUuid(
                    eq(USER_UUID), eq("UpdatedNick"), eq("Updated bio"),
                    eq("https://img.test.com/new.jpg"), eq("https://example.com")
            )).thenReturn(response);

            // when & then
            mockMvc.perform(put(BASE_URL + "/me/profile")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.nickname").value("UpdatedNick"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/users/me/username")
    class SetUsername {

        @Test
        @DisplayName("should_returnProfile_when_validUsername")
        void should_returnProfile_when_validUsername() throws Exception {
            // given
            UsernameSetRequest request = new UsernameSetRequest("new_username");

            UserProfileResponse response = createProfileResponse();
            when(userService.setUsernameByUuid(USER_UUID, "new_username")).thenReturn(response);

            // when & then
            mockMvc.perform(post(BASE_URL + "/me/username")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.uuid").value(USER_UUID));
        }

        @Test
        @DisplayName("should_returnBadRequest_when_usernameIsBlank")
        void should_returnBadRequest_when_usernameIsBlank() throws Exception {
            // given
            UsernameSetRequest request = new UsernameSetRequest("");

            // when & then
            mockMvc.perform(post(BASE_URL + "/me/username")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/check-username/{username}")
    class CheckUsername {

        @Test
        @DisplayName("should_returnAvailable_when_usernameNotTaken")
        void should_returnAvailable_when_usernameNotTaken() throws Exception {
            // given
            when(userService.checkUsernameAvailability("newuser")).thenReturn(true);

            // when & then
            mockMvc.perform(get(BASE_URL + "/check-username/newuser"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.username").value("newuser"))
                    .andExpect(jsonPath("$.data.available").value(true));
        }

        @Test
        @DisplayName("should_returnNotAvailable_when_usernameTaken")
        void should_returnNotAvailable_when_usernameTaken() throws Exception {
            // given
            when(userService.checkUsernameAvailability("taken")).thenReturn(false);

            // when & then
            mockMvc.perform(get(BASE_URL + "/check-username/taken"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.available").value(false));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/users/me/password")
    class ChangePassword {

        @Test
        @DisplayName("should_returnSuccess_when_validPasswordChange")
        void should_returnSuccess_when_validPasswordChange() throws Exception {
            // given
            PasswordChangeRequest request = new PasswordChangeRequest(
                    "currentPass1!", "newPass1!", "newPass1!"
            );
            doNothing().when(userService).changePasswordByUuid(USER_UUID, "currentPass1!", "newPass1!");

            // when & then
            mockMvc.perform(put(BASE_URL + "/me/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").value("Password changed successfully"));
        }

        @Test
        @DisplayName("should_returnError_when_passwordMismatch")
        void should_returnError_when_passwordMismatch() throws Exception {
            // given
            PasswordChangeRequest request = new PasswordChangeRequest(
                    "currentPass1!", "newPass1!", "differentPass!"
            );

            // when & then
            mockMvc.perform(put(BASE_URL + "/me/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }
}
