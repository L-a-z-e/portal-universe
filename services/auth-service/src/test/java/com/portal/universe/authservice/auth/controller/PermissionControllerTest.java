package com.portal.universe.authservice.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.universe.authservice.auth.dto.rbac.UserPermissionsResponse;
import com.portal.universe.authservice.auth.service.RbacService;
import com.portal.universe.authservice.common.exception.AuthErrorCode;
import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PermissionController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("PermissionController Unit Test")
class PermissionControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    RbacService rbacService;

    private static final String USER_UUID = "test-user-uuid";
    private static final String BASE_URL = "/api/v1/permissions";

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

    @Nested
    @DisplayName("GET /api/v1/permissions/me")
    class GetMyPermissions {

        @Test
        @DisplayName("should_returnPermissions_when_authenticated")
        void should_returnPermissions_when_authenticated() throws Exception {
            // given
            UserPermissionsResponse response = new UserPermissionsResponse(
                    USER_UUID,
                    List.of("ROLE_USER"),
                    List.of("blog:read", "shopping:read"),
                    Map.of("blog", "free", "shopping", "basic")
            );
            when(rbacService.resolveUserPermissions(USER_UUID)).thenReturn(response);

            // when & then
            mockMvc.perform(get(BASE_URL + "/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.userId").value(USER_UUID))
                    .andExpect(jsonPath("$.data.roles[0]").value("ROLE_USER"))
                    .andExpect(jsonPath("$.data.permissions").isArray())
                    .andExpect(jsonPath("$.data.permissions.length()").value(2))
                    .andExpect(jsonPath("$.data.memberships.blog").value("free"));
        }

        @Test
        @DisplayName("should_returnEmptyPermissions_when_noRolesAssigned")
        void should_returnEmptyPermissions_when_noRolesAssigned() throws Exception {
            // given
            UserPermissionsResponse response = new UserPermissionsResponse(
                    USER_UUID, List.of(), List.of(), Map.of()
            );
            when(rbacService.resolveUserPermissions(USER_UUID)).thenReturn(response);

            // when & then
            mockMvc.perform(get(BASE_URL + "/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.userId").value(USER_UUID))
                    .andExpect(jsonPath("$.data.roles").isEmpty())
                    .andExpect(jsonPath("$.data.permissions").isEmpty());
        }
    }

    @Nested
    @DisplayName("Security - 인증 검증")
    class SecurityTest {

        @Test
        @DisplayName("should_return401_when_notAuthenticated")
        void should_return401_when_notAuthenticated() throws Exception {
            // given - Clear authentication
            SecurityContextHolder.clearContext();

            // When no authentication, @AuthenticationPrincipal resolves to null
            // In production, security filters prevent reaching controller
            // Here we verify the controller doesn't return success without valid auth
            when(rbacService.resolveUserPermissions(null))
                    .thenThrow(new CustomBusinessException(AuthErrorCode.INVALID_TOKEN));

            // when & then
            mockMvc.perform(get(BASE_URL + "/me"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }
}
