package com.portal.universe.shoppingservice.timedeal.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.universe.commonlibrary.security.context.AuthUser;
import com.portal.universe.shoppingservice.timedeal.domain.TimeDealStatus;
import com.portal.universe.shoppingservice.timedeal.dto.TimeDealPurchaseRequest;
import com.portal.universe.shoppingservice.timedeal.dto.TimeDealPurchaseResponse;
import com.portal.universe.shoppingservice.timedeal.dto.TimeDealResponse;
import com.portal.universe.shoppingservice.timedeal.service.TimeDealService;
import com.portal.universe.shoppingservice.support.WebMvcTestConfig;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TimeDealController.class)
@ContextConfiguration(classes = {WebMvcTestConfig.class, TimeDealController.class})
@AutoConfigureMockMvc(addFilters = false)
class TimeDealControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TimeDealService timeDealService;

    private static final AuthUser authUser = new AuthUser("user-1", "Test User", "tester", null);

    private TimeDealResponse createTimeDealResponse() {
        return TimeDealResponse.builder()
                .id(1L)
                .name("Flash Sale")
                .description("Limited time offer")
                .status(TimeDealStatus.ACTIVE)
                .startsAt(LocalDateTime.now().minusHours(1))
                .endsAt(LocalDateTime.now().plusHours(2))
                .products(List.of())
                .createdAt(LocalDateTime.now())
                .build();
    }

    private TimeDealPurchaseResponse createPurchaseResponse() {
        return TimeDealPurchaseResponse.builder()
                .id(1L)
                .timeDealProductId(1L)
                .productName("Special Item")
                .quantity(1)
                .purchasePrice(BigDecimal.valueOf(5000))
                .totalPrice(BigDecimal.valueOf(5000))
                .purchasedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("should_returnActiveTimeDeals_when_called")
    void should_returnActiveTimeDeals_when_called() throws Exception {
        // given
        when(timeDealService.getActiveTimeDeals()).thenReturn(List.of(createTimeDealResponse()));

        // when/then
        mockMvc.perform(get("/time-deals").requestAttr("authUser", authUser))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Flash Sale"));
    }

    @Test
    @DisplayName("should_returnTimeDeal_when_getById")
    void should_returnTimeDeal_when_getById() throws Exception {
        // given
        when(timeDealService.getTimeDeal(1L)).thenReturn(createTimeDealResponse());

        // when/then
        mockMvc.perform(get("/time-deals/1").requestAttr("authUser", authUser))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("should_purchaseTimeDeal_when_validRequest")
    void should_purchaseTimeDeal_when_validRequest() throws Exception {
        // given
        TimeDealPurchaseRequest request = TimeDealPurchaseRequest.builder()
                .timeDealProductId(1L)
                .quantity(1)
                .build();
        when(timeDealService.purchaseTimeDeal(eq("user-1"), any(TimeDealPurchaseRequest.class)))
                .thenReturn(createPurchaseResponse());

        // when/then
        mockMvc.perform(post("/time-deals/purchase").requestAttr("authUser", authUser)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.productName").value("Special Item"));
    }

    @Test
    @DisplayName("should_returnMyPurchases_when_called")
    void should_returnMyPurchases_when_called() throws Exception {
        // given
        when(timeDealService.getUserPurchases("user-1")).thenReturn(List.of(createPurchaseResponse()));

        // when/then
        mockMvc.perform(get("/time-deals/my/purchases").requestAttr("authUser", authUser))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("should_returnBadRequest_when_invalidPurchaseRequest")
    void should_returnBadRequest_when_invalidPurchaseRequest() throws Exception {
        // when/then
        mockMvc.perform(post("/time-deals/purchase").requestAttr("authUser", authUser)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": 0}"))
                .andExpect(status().isBadRequest());
    }
}
