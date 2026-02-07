package com.portal.universe.authservice.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.universe.authservice.auth.domain.MembershipStatus;
import com.portal.universe.authservice.auth.dto.membership.ChangeMembershipRequest;
import com.portal.universe.authservice.auth.dto.membership.MembershipResponse;
import com.portal.universe.authservice.auth.dto.membership.MembershipTierResponse;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MembershipController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("MembershipController Unit Test")
class MembershipControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    MembershipService membershipService;

    @MockitoBean
    JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final String USER_UUID = "test-user-uuid";
    private static final String BASE_URL = "/api/v1/memberships";

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

    private MembershipResponse createMembershipResponse(String serviceName, String tierKey) {
        return new MembershipResponse(
                1L, USER_UUID, serviceName, tierKey, tierKey.toUpperCase(),
                MembershipStatus.ACTIVE, true,
                LocalDateTime.now(), LocalDateTime.now().plusMonths(1), LocalDateTime.now()
        );
    }

    @Nested
    @DisplayName("GET /api/v1/memberships/me")
    class GetMyMemberships {

        @Test
        @DisplayName("should_returnMembershipList_when_authenticated")
        void should_returnMembershipList_when_authenticated() throws Exception {
            // given
            List<MembershipResponse> responses = List.of(
                    createMembershipResponse("user:blog", "premium"),
                    createMembershipResponse("user:shopping", "basic")
            );
            when(membershipService.getUserMemberships(USER_UUID)).thenReturn(responses);

            // when & then
            mockMvc.perform(get(BASE_URL + "/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].membershipGroup").value("user:blog"));
        }

        @Test
        @DisplayName("should_returnEmptyList_when_noMemberships")
        void should_returnEmptyList_when_noMemberships() throws Exception {
            // given
            when(membershipService.getUserMemberships(USER_UUID)).thenReturn(List.of());

            // when & then
            mockMvc.perform(get(BASE_URL + "/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isEmpty());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/memberships/me/{serviceName}")
    class GetMyMembership {

        @Test
        @DisplayName("should_returnMembership_when_serviceNameProvided")
        void should_returnMembership_when_serviceNameProvided() throws Exception {
            // given
            MembershipResponse response = createMembershipResponse("user:blog", "premium");
            when(membershipService.getUserMembership(USER_UUID, "user:blog")).thenReturn(response);

            // when & then
            mockMvc.perform(get(BASE_URL + "/me/user:blog"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.membershipGroup").value("user:blog"))
                    .andExpect(jsonPath("$.data.tierKey").value("premium"))
                    .andExpect(jsonPath("$.data.status").value("ACTIVE"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/memberships/tiers/{serviceName}")
    class GetServiceTiers {

        @Test
        @DisplayName("should_returnTiers_when_serviceNameProvided")
        void should_returnTiers_when_serviceNameProvided() throws Exception {
            // given
            List<MembershipTierResponse> tiers = List.of(
                    new MembershipTierResponse(1L, "user:blog", "free", "Free", BigDecimal.ZERO, BigDecimal.ZERO, 0),
                    new MembershipTierResponse(2L, "user:blog", "premium", "Premium", new BigDecimal("9.99"), new BigDecimal("99.99"), 1)
            );
            when(membershipService.getGroupTiers("user:blog")).thenReturn(tiers);

            // when & then
            mockMvc.perform(get(BASE_URL + "/tiers/user:blog"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].tierKey").value("free"))
                    .andExpect(jsonPath("$.data[1].tierKey").value("premium"));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/memberships/me")
    class ChangeMembershipTier {

        @Test
        @DisplayName("should_returnUpdatedMembership_when_validRequest")
        void should_returnUpdatedMembership_when_validRequest() throws Exception {
            // given
            ChangeMembershipRequest request = new ChangeMembershipRequest("user:blog", "premium");
            MembershipResponse response = createMembershipResponse("user:blog", "premium");

            when(membershipService.changeMembershipTier(eq(USER_UUID), any(ChangeMembershipRequest.class)))
                    .thenReturn(response);

            // when & then
            mockMvc.perform(put(BASE_URL + "/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.membershipGroup").value("user:blog"))
                    .andExpect(jsonPath("$.data.tierKey").value("premium"));
        }

        @Test
        @DisplayName("should_returnBadRequest_when_serviceNameIsBlank")
        void should_returnBadRequest_when_serviceNameIsBlank() throws Exception {
            // given
            ChangeMembershipRequest request = new ChangeMembershipRequest("", "premium");

            // when & then
            mockMvc.perform(put(BASE_URL + "/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/memberships/me/{serviceName}")
    class CancelMembership {

        @Test
        @DisplayName("should_returnSuccess_when_validCancellation")
        void should_returnSuccess_when_validCancellation() throws Exception {
            // given
            doNothing().when(membershipService).cancelMembership(USER_UUID, "user:blog");

            // when & then
            mockMvc.perform(delete(BASE_URL + "/me/user:blog"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(membershipService).cancelMembership(USER_UUID, "user:blog");
        }
    }
}
