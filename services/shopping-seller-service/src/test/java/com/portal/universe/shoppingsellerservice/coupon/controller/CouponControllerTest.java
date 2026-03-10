package com.portal.universe.shoppingsellerservice.coupon.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.universe.shoppingsellerservice.coupon.domain.CouponStatus;
import com.portal.universe.shoppingsellerservice.coupon.domain.DiscountType;
import com.portal.universe.shoppingsellerservice.coupon.dto.CouponCreateRequest;
import com.portal.universe.shoppingsellerservice.coupon.dto.CouponResponse;
import com.portal.universe.shoppingsellerservice.coupon.service.CouponService;
import com.portal.universe.shoppingsellerservice.seller.dto.SellerResponse;
import com.portal.universe.shoppingsellerservice.seller.service.SellerService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CouponController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("CouponController")
class CouponControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CouponService couponService;

    @MockitoBean
    private SellerService sellerService;

    private static final String USER_ID = "seller-uuid";
    private static final Long SELLER_ID = 1L;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(USER_ID, null,
                        List.of(new SimpleGrantedAuthority("ROLE_SHOPPING_SELLER"))));

        SellerResponse sellerResponse = new SellerResponse(
                SELLER_ID, USER_ID, "Store", "123", "Rep", "010", "e@t.com",
                "Bank", "111", new BigDecimal("10.00"), "ACTIVE", null, null, null, null, Instant.now()
        );
        when(sellerService.getMyInfo(USER_ID)).thenReturn(sellerResponse);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static CouponResponse createCouponResponse() {
        return new CouponResponse(
                1L, SELLER_ID, "TEST-CODE", "Test Coupon", "desc",
                DiscountType.FIXED, new BigDecimal("1000"), new BigDecimal("10000"),
                new BigDecimal("5000"), 100, 0, CouponStatus.ACTIVE,
                Instant.now(), Instant.now().plus(30, ChronoUnit.DAYS),
                Instant.now(), Instant.now()
        );
    }

    @Nested
    @DisplayName("GET /coupons")
    class GetSellerCoupons {

        @Test
        @DisplayName("should return seller's coupons")
        void should_return_coupons() throws Exception {
            // given
            when(couponService.getSellerCoupons(eq(SELLER_ID), any()))
                    .thenReturn(new PageImpl<>(List.of(createCouponResponse())));

            // when & then
            mockMvc.perform(get("/coupons"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].code").value("TEST-CODE"));
        }
    }

    @Nested
    @DisplayName("GET /coupons/{couponId}")
    class GetCoupon {

        @Test
        @DisplayName("should return single coupon")
        void should_return_coupon() throws Exception {
            // given
            when(couponService.getCoupon(SELLER_ID, 1L)).thenReturn(createCouponResponse());

            // when & then
            mockMvc.perform(get("/coupons/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.name").value("Test Coupon"));
        }
    }

    @Nested
    @DisplayName("POST /coupons")
    class CreateCoupon {

        @Test
        @DisplayName("should create coupon")
        void should_create_coupon() throws Exception {
            // given
            CouponCreateRequest request = new CouponCreateRequest(
                    "NEW-CODE", "New Coupon", "desc", DiscountType.FIXED,
                    new BigDecimal("1000"), new BigDecimal("10000"), new BigDecimal("5000"),
                    100, Instant.now(), Instant.now().plus(30, ChronoUnit.DAYS)
            );
            when(couponService.createCoupon(eq(SELLER_ID), any(CouponCreateRequest.class)))
                    .thenReturn(createCouponResponse());

            // when & then
            mockMvc.perform(post("/coupons")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("DELETE /coupons/{couponId}")
    class DeactivateCoupon {

        @Test
        @DisplayName("should deactivate coupon")
        void should_deactivate_coupon() throws Exception {
            mockMvc.perform(delete("/coupons/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(couponService).deactivateCoupon(SELLER_ID, 1L);
        }
    }
}
