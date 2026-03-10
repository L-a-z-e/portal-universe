package com.portal.universe.shoppingsellerservice.seller.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.universe.shoppingsellerservice.seller.dto.SellerApplyRequest;
import com.portal.universe.shoppingsellerservice.seller.dto.SellerRegisterRequest;
import com.portal.universe.shoppingsellerservice.seller.dto.SellerResponse;
import com.portal.universe.shoppingsellerservice.seller.dto.SellerUpdateRequest;
import com.portal.universe.shoppingsellerservice.seller.service.SellerService;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SellerController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("SellerController")
class SellerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SellerService sellerService;

    private static final String USER_ID = "user-uuid-001";

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(USER_ID, null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static SellerResponse createSellerResponse() {
        return new SellerResponse(
                1L, USER_ID, "Test Store", "123-45-67890", "Rep", "010-1234-5678",
                "seller@test.com", "Test Bank", "111-222", new BigDecimal("10.00"),
                "PENDING", null, null, null, null, Instant.now()
        );
    }

    @Nested
    @DisplayName("POST /sellers/apply")
    class Apply {

        @Test
        @DisplayName("should apply as seller and return 201")
        void should_apply_as_seller() throws Exception {
            // given
            SellerApplyRequest request = new SellerApplyRequest(
                    "New Store", "123-45-67890", "Rep", "010", "e@t.com", "Bank", "111", "I want to sell"
            );
            when(sellerService.apply(eq(USER_ID), any(SellerApplyRequest.class)))
                    .thenReturn(createSellerResponse());

            // when & then
            mockMvc.perform(post("/sellers/apply")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.businessName").value("Test Store"));
        }
    }

    @Nested
    @DisplayName("GET /sellers/my-application")
    class GetMyApplication {

        @Test
        @DisplayName("should return seller application")
        void should_return_application() throws Exception {
            // given
            when(sellerService.getMyApplication(USER_ID)).thenReturn(createSellerResponse());

            // when & then
            mockMvc.perform(get("/sellers/my-application"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.userId").value(USER_ID))
                    .andExpect(jsonPath("$.data.status").value("PENDING"));
        }
    }

    @Nested
    @DisplayName("POST /sellers/register")
    class Register {

        @Test
        @DisplayName("should register as seller")
        void should_register_seller() throws Exception {
            // given
            SellerRegisterRequest request = new SellerRegisterRequest(
                    "Store", "123", "Rep", "010", "e@t.com", "Bank", "111"
            );
            when(sellerService.register(eq(USER_ID), any(SellerRegisterRequest.class)))
                    .thenReturn(createSellerResponse());

            // when & then
            mockMvc.perform(post("/sellers/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("GET /sellers/me")
    class GetMyInfo {

        @Test
        @DisplayName("should return seller info")
        void should_return_info() throws Exception {
            // given
            when(sellerService.getMyInfo(USER_ID)).thenReturn(createSellerResponse());

            // when & then
            mockMvc.perform(get("/sellers/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.businessName").value("Test Store"));
        }
    }

    @Nested
    @DisplayName("PUT /sellers/me")
    class Update {

        @Test
        @DisplayName("should update seller info")
        void should_update_seller() throws Exception {
            // given
            SellerUpdateRequest request = new SellerUpdateRequest(
                    "Updated Store", "010-9999", "updated@test.com", "New Bank", "999"
            );
            SellerResponse updated = new SellerResponse(
                    1L, USER_ID, "Updated Store", "123-45-67890", "Rep", "010-9999",
                    "updated@test.com", "New Bank", "999", new BigDecimal("10.00"),
                    "PENDING", null, null, null, null, Instant.now()
            );
            when(sellerService.update(eq(USER_ID), any(SellerUpdateRequest.class)))
                    .thenReturn(updated);

            // when & then
            mockMvc.perform(put("/sellers/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.businessName").value("Updated Store"));
        }
    }
}
