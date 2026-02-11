package com.portal.universe.authservice.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.universe.authservice.auth.domain.SellerApplicationStatus;
import com.portal.universe.authservice.auth.dto.seller.SellerApplicationResponse;
import com.portal.universe.authservice.auth.dto.seller.SellerApplicationReviewRequest;
import com.portal.universe.authservice.auth.service.SellerApplicationService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SellerAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("SellerAdminController Unit Test")
@Import(SellerAdminControllerTest.TestMethodSecurityConfig.class)
class SellerAdminControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class TestMethodSecurityConfig {}

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    SellerApplicationService sellerApplicationService;

    private static final String ADMIN_UUID = "admin-uuid";
    private static final String BASE_URL = "/api/v1/admin/seller";

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

    private SellerApplicationResponse createApplicationResponse(
            Long id, String userId, SellerApplicationStatus status) {
        return new SellerApplicationResponse(
                id, userId, "Business " + id, "123-45-" + id,
                "Want to sell", status, null, null, null, LocalDateTime.now()
        );
    }

    @Nested
    @DisplayName("GET /api/v1/admin/seller/applications/pending")
    class GetPendingApplications {

        @Test
        @DisplayName("should_returnPendingApplications_when_adminRequest")
        void should_returnPendingApplications_when_adminRequest() throws Exception {
            // given
            List<SellerApplicationResponse> applications = List.of(
                    createApplicationResponse(1L, "user-1", SellerApplicationStatus.PENDING),
                    createApplicationResponse(2L, "user-2", SellerApplicationStatus.PENDING)
            );
            Page<SellerApplicationResponse> page = new PageImpl<>(
                    applications, PageRequest.of(0, 20), 2
            );
            when(sellerApplicationService.getPendingApplications(any())).thenReturn(page);

            // when & then
            mockMvc.perform(get(BASE_URL + "/applications/pending"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.items").isArray())
                    .andExpect(jsonPath("$.data.items.length()").value(2))
                    .andExpect(jsonPath("$.data.items[0].status").value("PENDING"))
                    .andExpect(jsonPath("$.data.totalElements").value(2));
        }

        @Test
        @DisplayName("should_returnEmptyPage_when_noPendingApplications")
        void should_returnEmptyPage_when_noPendingApplications() throws Exception {
            // given
            Page<SellerApplicationResponse> emptyPage = new PageImpl<>(
                    List.of(), PageRequest.of(0, 20), 0
            );
            when(sellerApplicationService.getPendingApplications(any())).thenReturn(emptyPage);

            // when & then
            mockMvc.perform(get(BASE_URL + "/applications/pending"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.items").isEmpty())
                    .andExpect(jsonPath("$.data.totalElements").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/seller/applications")
    class GetAllApplications {

        @Test
        @DisplayName("should_returnAllApplications_when_adminRequest")
        void should_returnAllApplications_when_adminRequest() throws Exception {
            // given
            List<SellerApplicationResponse> applications = List.of(
                    createApplicationResponse(1L, "user-1", SellerApplicationStatus.PENDING),
                    createApplicationResponse(2L, "user-2", SellerApplicationStatus.APPROVED),
                    createApplicationResponse(3L, "user-3", SellerApplicationStatus.REJECTED)
            );
            Page<SellerApplicationResponse> page = new PageImpl<>(
                    applications, PageRequest.of(0, 20), 3
            );
            when(sellerApplicationService.getAllApplications(any())).thenReturn(page);

            // when & then
            mockMvc.perform(get(BASE_URL + "/applications"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.items.length()").value(3))
                    .andExpect(jsonPath("$.data.totalElements").value(3));
        }

        @Test
        @DisplayName("should_supportPagination_when_pageParamsProvided")
        void should_supportPagination_when_pageParamsProvided() throws Exception {
            // given
            List<SellerApplicationResponse> applications = List.of(
                    createApplicationResponse(11L, "user-11", SellerApplicationStatus.PENDING)
            );
            Page<SellerApplicationResponse> page = new PageImpl<>(
                    applications, PageRequest.of(1, 10), 11
            );
            when(sellerApplicationService.getAllApplications(any())).thenReturn(page);

            // when & then
            mockMvc.perform(get(BASE_URL + "/applications")
                            .param("page", "1")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.items.length()").value(1))
                    .andExpect(jsonPath("$.data.totalElements").value(11));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/seller/applications/{applicationId}/review")
    class Review {

        @Test
        @DisplayName("should_returnApprovedApplication_when_approved")
        void should_returnApprovedApplication_when_approved() throws Exception {
            // given
            SellerApplicationReviewRequest request = new SellerApplicationReviewRequest(true, "Approved");
            SellerApplicationResponse response = new SellerApplicationResponse(
                    1L, "user-1", "Test Business", "123-45-67890",
                    "Want to sell", SellerApplicationStatus.APPROVED,
                    ADMIN_UUID, "Approved", LocalDateTime.now(), LocalDateTime.now()
            );

            when(sellerApplicationService.review(eq(1L), any(SellerApplicationReviewRequest.class), eq(ADMIN_UUID)))
                    .thenReturn(response);

            // when & then
            mockMvc.perform(post(BASE_URL + "/applications/1/review")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("APPROVED"))
                    .andExpect(jsonPath("$.data.reviewedBy").value(ADMIN_UUID))
                    .andExpect(jsonPath("$.data.reviewComment").value("Approved"));
        }

        @Test
        @DisplayName("should_returnRejectedApplication_when_rejected")
        void should_returnRejectedApplication_when_rejected() throws Exception {
            // given
            SellerApplicationReviewRequest request = new SellerApplicationReviewRequest(false, "Incomplete documents");
            SellerApplicationResponse response = new SellerApplicationResponse(
                    1L, "user-1", "Test Business", "123-45-67890",
                    "Want to sell", SellerApplicationStatus.REJECTED,
                    ADMIN_UUID, "Incomplete documents", LocalDateTime.now(), LocalDateTime.now()
            );

            when(sellerApplicationService.review(eq(1L), any(SellerApplicationReviewRequest.class), eq(ADMIN_UUID)))
                    .thenReturn(response);

            // when & then
            mockMvc.perform(post(BASE_URL + "/applications/1/review")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("REJECTED"))
                    .andExpect(jsonPath("$.data.reviewComment").value("Incomplete documents"));
        }

        @Test
        @DisplayName("should_returnBadRequest_when_approvedFieldIsNull")
        void should_returnBadRequest_when_approvedFieldIsNull() throws Exception {
            // given - approved is null
            String requestBody = "{\"reviewComment\": \"comment\"}";

            // when & then
            mockMvc.perform(post(BASE_URL + "/applications/1/review")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Security - 권한 검증")
    class SecurityTest {

        @Test
        @DisplayName("should_return403_when_notAdmin")
        void should_return403_when_notAdmin() throws Exception {
            // given - ROLE_USER only (not ROLE_SHOPPING_ADMIN or ROLE_SUPER_ADMIN)
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                            "regular-user", null,
                            List.of(new SimpleGrantedAuthority("ROLE_USER"))
                    )
            );

            // when & then - @PreAuthorize denies access, throwing AccessDeniedException
            mockMvc.perform(get(BASE_URL + "/applications/pending"))
                    .andExpect(result ->
                            assertThat(result.getResolvedException())
                                    .isInstanceOf(AccessDeniedException.class)
                    );
        }
    }
}
