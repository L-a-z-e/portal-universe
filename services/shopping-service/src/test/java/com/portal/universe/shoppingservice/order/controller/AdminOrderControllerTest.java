package com.portal.universe.shoppingservice.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.universe.shoppingservice.order.domain.OrderStatus;
import com.portal.universe.shoppingservice.order.dto.OrderResponse;
import com.portal.universe.shoppingservice.order.service.AdminOrderService;
import com.portal.universe.shoppingservice.support.WebMvcTestConfig;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Disabled;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// AdminOrderController is not yet implemented in shopping-service.
// This test is disabled until the controller is created.
@Disabled("AdminOrderController not yet implemented")
@WebMvcTest
@AutoConfigureMockMvc(addFilters = false)
class AdminOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdminOrderService adminOrderService;

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

    private OrderResponse createOrderResponse() {
        return new OrderResponse(1L, "ORD-001", "user-1", OrderStatus.PENDING,
                "주문 대기", List.of(), 1, 2, BigDecimal.valueOf(20000),
                BigDecimal.ZERO, BigDecimal.valueOf(20000), null, null,
                null, null, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    @DisplayName("should_returnOrders_when_getOrders")
    void should_returnOrders_when_getOrders() throws Exception {
        // given
        OrderResponse response = createOrderResponse();
        Page<OrderResponse> page = new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1);
        when(adminOrderService.getOrders(any(), any(), any())).thenReturn(page);

        // when/then
        mockMvc.perform(get("/admin/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].orderNumber").value("ORD-001"));
    }

    @Test
    @DisplayName("should_returnOrder_when_getOrderByNumber")
    void should_returnOrder_when_getOrderByNumber() throws Exception {
        // given
        OrderResponse response = createOrderResponse();
        when(adminOrderService.getOrder("ORD-001")).thenReturn(response);

        // when/then
        mockMvc.perform(get("/admin/orders/ORD-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderNumber").value("ORD-001"));
    }

    @Test
    @DisplayName("should_updateOrderStatus_when_validRequest")
    void should_updateOrderStatus_when_validRequest() throws Exception {
        // given
        OrderResponse response = createOrderResponse();
        when(adminOrderService.updateOrderStatus("ORD-001", "CONFIRMED")).thenReturn(response);

        // when/then
        mockMvc.perform(put("/admin/orders/ORD-001/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"CONFIRMED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("should_returnOrdersWithFilter_when_statusProvided")
    void should_returnOrdersWithFilter_when_statusProvided() throws Exception {
        // given
        Page<OrderResponse> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(adminOrderService.getOrders(eq("PENDING"), any(), any())).thenReturn(page);

        // when/then
        mockMvc.perform(get("/admin/orders").param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
