package com.portal.universe.shoppingservice.coupon.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.universe.commonlibrary.security.context.AuthUser;
import com.portal.universe.shoppingservice.coupon.domain.CouponStatus;
import com.portal.universe.shoppingservice.coupon.domain.DiscountType;
import com.portal.universe.shoppingservice.coupon.domain.UserCouponStatus;
import com.portal.universe.shoppingservice.coupon.dto.CouponResponse;
import com.portal.universe.shoppingservice.coupon.dto.UserCouponResponse;
import com.portal.universe.shoppingservice.coupon.service.CouponService;
import com.portal.universe.shoppingservice.support.WebMvcTestConfig;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CouponController.class)
@ContextConfiguration(classes = {WebMvcTestConfig.class, CouponController.class})
@AutoConfigureMockMvc(addFilters = false)
class CouponControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CouponService couponService;

    private static final AuthUser authUser = new AuthUser("user-1", "Test User", "tester", null);

    private CouponResponse createCouponResponse() {
        return CouponResponse.builder()
                .id(1L)
                .code("WELCOME10")
                .name("Welcome Coupon")
                .description("10% discount")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(BigDecimal.TEN)
                .minimumOrderAmount(BigDecimal.valueOf(10000))
                .maximumDiscountAmount(BigDecimal.valueOf(5000))
                .totalQuantity(100)
                .issuedQuantity(10)
                .remainingQuantity(90)
                .status(CouponStatus.ACTIVE)
                .startsAt(LocalDateTime.now().minusDays(1))
                .expiresAt(LocalDateTime.now().plusDays(30))
                .createdAt(LocalDateTime.now())
                .build();
    }

    private UserCouponResponse createUserCouponResponse() {
        return UserCouponResponse.builder()
                .id(1L)
                .couponId(1L)
                .couponCode("WELCOME10")
                .couponName("Welcome Coupon")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(BigDecimal.TEN)
                .minimumOrderAmount(BigDecimal.valueOf(10000))
                .maximumDiscountAmount(BigDecimal.valueOf(5000))
                .status(UserCouponStatus.AVAILABLE)
                .issuedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build();
    }

    @Test
    @DisplayName("should_returnAvailableCoupons_when_called")
    void should_returnAvailableCoupons_when_called() throws Exception {
        // given
        when(couponService.getAvailableCoupons()).thenReturn(List.of(createCouponResponse()));

        // when/then
        mockMvc.perform(get("/coupons").requestAttr("authUser", authUser))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].code").value("WELCOME10"));
    }

    @Test
    @DisplayName("should_returnCoupon_when_getCouponById")
    void should_returnCoupon_when_getCouponById() throws Exception {
        // given
        when(couponService.getCoupon(1L)).thenReturn(createCouponResponse());

        // when/then
        mockMvc.perform(get("/coupons/1").requestAttr("authUser", authUser))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("should_issueCoupon_when_called")
    void should_issueCoupon_when_called() throws Exception {
        // given
        when(couponService.issueCoupon(1L, "user-1")).thenReturn(createUserCouponResponse());

        // when/then
        mockMvc.perform(post("/coupons/1/issue").requestAttr("authUser", authUser))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.couponCode").value("WELCOME10"));
    }

    @Test
    @DisplayName("should_returnMyCoupons_when_called")
    void should_returnMyCoupons_when_called() throws Exception {
        // given
        when(couponService.getUserCoupons("user-1")).thenReturn(List.of(createUserCouponResponse()));

        // when/then
        mockMvc.perform(get("/coupons/my").requestAttr("authUser", authUser))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("should_returnMyAvailableCoupons_when_called")
    void should_returnMyAvailableCoupons_when_called() throws Exception {
        // given
        when(couponService.getAvailableUserCoupons("user-1")).thenReturn(List.of(createUserCouponResponse()));

        // when/then
        mockMvc.perform(get("/coupons/my/available").requestAttr("authUser", authUser))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("should_returnEmptyList_when_noAvailableCoupons")
    void should_returnEmptyList_when_noAvailableCoupons() throws Exception {
        // given
        when(couponService.getAvailableCoupons()).thenReturn(List.of());

        // when/then
        mockMvc.perform(get("/coupons").requestAttr("authUser", authUser))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }
}
