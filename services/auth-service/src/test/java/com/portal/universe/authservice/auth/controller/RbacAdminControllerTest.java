package com.portal.universe.authservice.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.universe.authservice.auth.dto.rbac.AssignRoleRequest;
import com.portal.universe.authservice.auth.dto.rbac.RoleResponse;
import com.portal.universe.authservice.auth.dto.rbac.UserPermissionsResponse;
import com.portal.universe.authservice.auth.dto.rbac.UserRoleResponse;
import com.portal.universe.authservice.auth.service.RbacService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RbacAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("RbacAdminController Unit Test")
@Import(RbacAdminControllerTest.TestMethodSecurityConfig.class)
class RbacAdminControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class TestMethodSecurityConfig {}

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    RbacService rbacService;

    private static final String ADMIN_UUID = "admin-uuid";
    private static final String TARGET_USER_UUID = "target-user-uuid";
    private static final String BASE_URL = "/api/v1/admin/rbac";

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        ADMIN_UUID, null,
                        List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))
                )
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("GET /api/v1/admin/rbac/roles")
    class GetAllRoles {

        @Test
        @DisplayName("should_returnAllActiveRoles_when_adminRequest")
        void should_returnAllActiveRoles_when_adminRequest() throws Exception {
            // given
            List<RoleResponse> roles = List.of(
                    new RoleResponse(1L, "ROLE_USER", "User", "Basic user role", "global", null, null, true, true),
                    new RoleResponse(2L, "ROLE_SUPER_ADMIN", "Super Admin", "Full access", "global", null, null, true, true),
                    new RoleResponse(3L, "ROLE_BLOG_ADMIN", "Blog Admin", "Blog admin role", "blog", null, null, false, true)
            );
            when(rbacService.getAllActiveRoles()).thenReturn(roles);

            // when & then
            mockMvc.perform(get(BASE_URL + "/roles"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(3))
                    .andExpect(jsonPath("$.data[0].roleKey").value("ROLE_USER"));
        }

        @Test
        @DisplayName("should_returnEmptyList_when_noActiveRoles")
        void should_returnEmptyList_when_noActiveRoles() throws Exception {
            // given
            when(rbacService.getAllActiveRoles()).thenReturn(List.of());

            // when & then
            mockMvc.perform(get(BASE_URL + "/roles"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isEmpty());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/rbac/users/{userId}/roles")
    class GetUserRoles {

        @Test
        @DisplayName("should_returnUserRoles_when_validUserId")
        void should_returnUserRoles_when_validUserId() throws Exception {
            // given
            List<UserRoleResponse> userRoles = List.of(
                    new UserRoleResponse(1L, "ROLE_USER", "User", ADMIN_UUID, LocalDateTime.now(), null),
                    new UserRoleResponse(2L, "ROLE_BLOG_ADMIN", "Blog Admin", ADMIN_UUID, LocalDateTime.now(), null)
            );
            when(rbacService.getUserRoles(TARGET_USER_UUID)).thenReturn(userRoles);

            // when & then
            mockMvc.perform(get(BASE_URL + "/users/" + TARGET_USER_UUID + "/roles"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].roleKey").value("ROLE_USER"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/rbac/users/{userId}/permissions")
    class GetUserPermissions {

        @Test
        @DisplayName("should_returnUserPermissions_when_validUserId")
        void should_returnUserPermissions_when_validUserId() throws Exception {
            // given
            UserPermissionsResponse response = new UserPermissionsResponse(
                    TARGET_USER_UUID,
                    List.of("ROLE_USER", "ROLE_BLOG_ADMIN"),
                    List.of("blog:read", "blog:write", "blog:delete"),
                    Map.of("blog", "premium")
            );
            when(rbacService.resolveUserPermissions(TARGET_USER_UUID)).thenReturn(response);

            // when & then
            mockMvc.perform(get(BASE_URL + "/users/" + TARGET_USER_UUID + "/permissions"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.userId").value(TARGET_USER_UUID))
                    .andExpect(jsonPath("$.data.roles").isArray())
                    .andExpect(jsonPath("$.data.permissions").isArray())
                    .andExpect(jsonPath("$.data.memberships.blog").value("premium"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/rbac/roles/assign")
    class AssignRole {

        @Test
        @DisplayName("should_returnCreated_when_validAssignment")
        void should_returnCreated_when_validAssignment() throws Exception {
            // given
            AssignRoleRequest request = new AssignRoleRequest(TARGET_USER_UUID, "ROLE_BLOG_ADMIN", null);
            UserRoleResponse response = new UserRoleResponse(
                    1L, "ROLE_BLOG_ADMIN", "Blog Admin", ADMIN_UUID, LocalDateTime.now(), null
            );

            when(rbacService.assignRole(any(AssignRoleRequest.class), eq(ADMIN_UUID))).thenReturn(response);

            // when & then
            mockMvc.perform(post(BASE_URL + "/roles/assign")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.roleKey").value("ROLE_BLOG_ADMIN"))
                    .andExpect(jsonPath("$.data.assignedBy").value(ADMIN_UUID));
        }

        @Test
        @DisplayName("should_returnBadRequest_when_userIdIsBlank")
        void should_returnBadRequest_when_userIdIsBlank() throws Exception {
            // given
            AssignRoleRequest request = new AssignRoleRequest("", "ROLE_BLOG_ADMIN", null);

            // when & then
            mockMvc.perform(post(BASE_URL + "/roles/assign")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should_returnBadRequest_when_roleKeyIsBlank")
        void should_returnBadRequest_when_roleKeyIsBlank() throws Exception {
            // given
            AssignRoleRequest request = new AssignRoleRequest(TARGET_USER_UUID, "", null);

            // when & then
            mockMvc.perform(post(BASE_URL + "/roles/assign")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/admin/rbac/users/{userId}/roles/{roleKey}")
    class RevokeRole {

        @Test
        @DisplayName("should_returnSuccess_when_validRevocation")
        void should_returnSuccess_when_validRevocation() throws Exception {
            // given
            doNothing().when(rbacService).revokeRole(TARGET_USER_UUID, "ROLE_BLOG_ADMIN", ADMIN_UUID);

            // when & then
            mockMvc.perform(delete(BASE_URL + "/users/" + TARGET_USER_UUID + "/roles/ROLE_BLOG_ADMIN"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(rbacService).revokeRole(TARGET_USER_UUID, "ROLE_BLOG_ADMIN", ADMIN_UUID);
        }
    }

    @Nested
    @DisplayName("Security - 권한 검증")
    class SecurityTest {

        @Test
        @DisplayName("should_return403_when_notSuperAdmin")
        void should_return403_when_notSuperAdmin() throws Exception {
            // given - ROLE_USER only (not ROLE_SUPER_ADMIN)
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                            "regular-user", null,
                            List.of(new SimpleGrantedAuthority("ROLE_USER"))
                    )
            );

            // when & then - @PreAuthorize denies access, throwing AccessDeniedException
            mockMvc.perform(get(BASE_URL + "/roles"))
                    .andExpect(result ->
                            assertThat(result.getResolvedException())
                                    .isInstanceOf(AccessDeniedException.class)
                    );
        }
    }
}
