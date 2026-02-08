package com.portal.universe.shoppingservice.coupon.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.universe.shoppingservice.coupon.domain.CouponStatus;
import com.portal.universe.shoppingservice.coupon.domain.DiscountType;
import com.portal.universe.shoppingservice.coupon.dto.CouponCreateRequest;
import com.portal.universe.shoppingservice.coupon.dto.CouponResponse;
import com.portal.universe.shoppingservice.coupon.service.CouponService;
import com.portal.universe.shoppingservice.support.WebMvcTestConfig;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminCouponController.class)
@ContextConfiguration(classes = {WebMvcTestConfig.class, AdminCouponController.class})
@AutoConfigureMockMvc(addFilters = false)
class AdminCouponControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CouponService couponService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user-1", null, List.of())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

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

    @Test
    @DisplayName("should_returnCoupons_when_getCoupons")
    void should_returnCoupons_when_getCoupons() throws Exception {
        // given
        Page<CouponResponse> page = new PageImpl<>(List.of(createCouponResponse()), PageRequest.of(0, 10), 1);
        when(couponService.getAllCoupons(any())).thenReturn(page);

        // when/then
        mockMvc.perform(get("/admin/coupons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].code").value("WELCOME10"));
    }

    @Test
    @DisplayName("should_createCoupon_when_validRequest")
    void should_createCoupon_when_validRequest() throws Exception {
        // given
        CouponCreateRequest request = CouponCreateRequest.builder()
                .code("SUMMER20")
                .name("Summer Sale")
                .description("20% discount")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(20))
                .minimumOrderAmount(BigDecimal.valueOf(10000))
                .maximumDiscountAmount(BigDecimal.valueOf(10000))
                .totalQuantity(500)
                .startsAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build();
        when(couponService.createCoupon(any(CouponCreateRequest.class))).thenReturn(createCouponResponse());

        // when/then
        mockMvc.perform(post("/admin/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("should_returnCoupon_when_getCouponById")
    void should_returnCoupon_when_getCouponById() throws Exception {
        // given
        when(couponService.getCoupon(1L)).thenReturn(createCouponResponse());

        // when/then
        mockMvc.perform(get("/admin/coupons/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("should_deactivateCoupon_when_called")
    void should_deactivateCoupon_when_called() throws Exception {
        // given
        doNothing().when(couponService).deactivateCoupon(1L);

        // when/then
        mockMvc.perform(delete("/admin/coupons/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        verify(couponService).deactivateCoupon(1L);
    }

    @Test
    @DisplayName("should_returnBadRequest_when_invalidCreateRequest")
    void should_returnBadRequest_when_invalidCreateRequest() throws Exception {
        // when/then
        mockMvc.perform(post("/admin/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
