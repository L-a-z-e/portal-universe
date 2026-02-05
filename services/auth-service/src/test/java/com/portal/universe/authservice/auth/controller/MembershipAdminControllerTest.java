package com.portal.universe.authservice.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.universe.authservice.auth.domain.MembershipStatus;
import com.portal.universe.authservice.auth.dto.membership.ChangeMembershipRequest;
import com.portal.universe.authservice.auth.dto.membership.MembershipResponse;
import com.portal.universe.authservice.auth.service.MembershipService;
import com.portal.universe.authservice.auth.security.JwtAuthenticationFilter;
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

import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MembershipAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("MembershipAdminController Unit Test")
@Import(MembershipAdminControllerTest.TestMethodSecurityConfig.class)
class MembershipAdminControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class TestMethodSecurityConfig {}

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    MembershipService membershipService;

    @MockitoBean
    JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final String ADMIN_UUID = "admin-uuid";
    private static final String TARGET_USER_UUID = "target-user-uuid";
    private static final String BASE_URL = "/api/v1/admin/memberships";

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

    private MembershipResponse createMembershipResponse(String userId, String serviceName, String tierKey) {
        return new MembershipResponse(
                1L, userId, serviceName, tierKey, tierKey.toUpperCase(),
                MembershipStatus.ACTIVE, true,
                LocalDateTime.now(), LocalDateTime.now().plusMonths(1), LocalDateTime.now()
        );
    }

    @Nested
    @DisplayName("GET /api/v1/admin/memberships/users/{userId}")
    class GetUserMemberships {

        @Test
        @DisplayName("should_returnUserMemberships_when_adminRequest")
        void should_returnUserMemberships_when_adminRequest() throws Exception {
            // given
            List<MembershipResponse> responses = List.of(
                    createMembershipResponse(TARGET_USER_UUID, "blog", "premium")
            );
            when(membershipService.getUserMemberships(TARGET_USER_UUID)).thenReturn(responses);

            // when & then
            mockMvc.perform(get(BASE_URL + "/users/" + TARGET_USER_UUID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].userId").value(TARGET_USER_UUID))
                    .andExpect(jsonPath("$.data[0].serviceName").value("blog"));
        }

        @Test
        @DisplayName("should_returnEmptyList_when_userHasNoMemberships")
        void should_returnEmptyList_when_userHasNoMemberships() throws Exception {
            // given
            when(membershipService.getUserMemberships(TARGET_USER_UUID)).thenReturn(List.of());

            // when & then
            mockMvc.perform(get(BASE_URL + "/users/" + TARGET_USER_UUID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isEmpty());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/admin/memberships/users/{userId}")
    class ChangeMembershipTier {

        @Test
        @DisplayName("should_returnUpdatedMembership_when_validAdminRequest")
        void should_returnUpdatedMembership_when_validAdminRequest() throws Exception {
            // given
            ChangeMembershipRequest request = new ChangeMembershipRequest("blog", "enterprise");
            MembershipResponse response = createMembershipResponse(TARGET_USER_UUID, "blog", "enterprise");

            when(membershipService.adminChangeMembershipTier(
                    eq(TARGET_USER_UUID), any(ChangeMembershipRequest.class), eq(ADMIN_UUID)
            )).thenReturn(response);

            // when & then
            mockMvc.perform(put(BASE_URL + "/users/" + TARGET_USER_UUID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.userId").value(TARGET_USER_UUID))
                    .andExpect(jsonPath("$.data.tierKey").value("enterprise"));
        }

        @Test
        @DisplayName("should_returnBadRequest_when_tierKeyIsBlank")
        void should_returnBadRequest_when_tierKeyIsBlank() throws Exception {
            // given
            ChangeMembershipRequest request = new ChangeMembershipRequest("blog", "");

            // when & then
            mockMvc.perform(put(BASE_URL + "/users/" + TARGET_USER_UUID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
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
            mockMvc.perform(get(BASE_URL + "/users/" + TARGET_USER_UUID))
                    .andExpect(result ->
                            assertThat(result.getResolvedException())
                                    .isInstanceOf(AccessDeniedException.class)
                    );
        }
    }
}
