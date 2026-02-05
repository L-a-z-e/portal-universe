package com.portal.universe.authservice.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.universe.authservice.auth.domain.SellerApplicationStatus;
import com.portal.universe.authservice.auth.dto.seller.SellerApplicationRequest;
import com.portal.universe.authservice.auth.dto.seller.SellerApplicationResponse;
import com.portal.universe.authservice.auth.service.SellerApplicationService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SellerController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("SellerController Unit Test")
class SellerControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    SellerApplicationService sellerApplicationService;

    @MockitoBean
    JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final String USER_UUID = "test-user-uuid";
    private static final String BASE_URL = "/api/v1/seller";

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

    private SellerApplicationResponse createApplicationResponse(SellerApplicationStatus status) {
        return new SellerApplicationResponse(
                1L, USER_UUID, "Test Business", "123-45-67890",
                "Want to sell", status, null, null, null, LocalDateTime.now()
        );
    }

    @Nested
    @DisplayName("POST /api/v1/seller/apply")
    class Apply {

        @Test
        @DisplayName("should_returnCreated_when_validApplication")
        void should_returnCreated_when_validApplication() throws Exception {
            // given
            SellerApplicationRequest request = new SellerApplicationRequest(
                    "Test Business", "123-45-67890", "Want to sell"
            );
            SellerApplicationResponse response = createApplicationResponse(SellerApplicationStatus.PENDING);

            when(sellerApplicationService.apply(eq(USER_UUID), any(SellerApplicationRequest.class)))
                    .thenReturn(response);

            // when & then
            mockMvc.perform(post(BASE_URL + "/apply")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.userId").value(USER_UUID))
                    .andExpect(jsonPath("$.data.businessName").value("Test Business"))
                    .andExpect(jsonPath("$.data.status").value("PENDING"));
        }

        @Test
        @DisplayName("should_returnBadRequest_when_businessNameIsBlank")
        void should_returnBadRequest_when_businessNameIsBlank() throws Exception {
            // given
            SellerApplicationRequest request = new SellerApplicationRequest("", "123-45-67890", "Want to sell");

            // when & then
            mockMvc.perform(post(BASE_URL + "/apply")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/seller/application")
    class GetMyApplication {

        @Test
        @DisplayName("should_returnApplication_when_exists")
        void should_returnApplication_when_exists() throws Exception {
            // given
            SellerApplicationResponse response = createApplicationResponse(SellerApplicationStatus.PENDING);
            when(sellerApplicationService.getMyApplication(USER_UUID)).thenReturn(response);

            // when & then
            mockMvc.perform(get(BASE_URL + "/application"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.userId").value(USER_UUID))
                    .andExpect(jsonPath("$.data.status").value("PENDING"));
        }

        @Test
        @DisplayName("should_returnApprovedApplication_when_reviewed")
        void should_returnApprovedApplication_when_reviewed() throws Exception {
            // given
            SellerApplicationResponse response = new SellerApplicationResponse(
                    1L, USER_UUID, "Test Business", "123-45-67890",
                    "Want to sell", SellerApplicationStatus.APPROVED,
                    "admin-uuid", "Looks good", LocalDateTime.now(), LocalDateTime.now()
            );
            when(sellerApplicationService.getMyApplication(USER_UUID)).thenReturn(response);

            // when & then
            mockMvc.perform(get(BASE_URL + "/application"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("APPROVED"))
                    .andExpect(jsonPath("$.data.reviewComment").value("Looks good"));
        }
    }
}
