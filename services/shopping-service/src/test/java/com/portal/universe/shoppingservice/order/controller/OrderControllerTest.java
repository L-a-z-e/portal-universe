package com.portal.universe.shoppingservice.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.universe.commonlibrary.security.context.AuthUser;
import com.portal.universe.shoppingservice.order.domain.OrderStatus;
import com.portal.universe.shoppingservice.order.dto.*;
import com.portal.universe.shoppingservice.order.service.OrderService;
import com.portal.universe.shoppingservice.support.WebMvcTestConfig;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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

@WebMvcTest(OrderController.class)
@ContextConfiguration(classes = {WebMvcTestConfig.class, OrderController.class})
@AutoConfigureMockMvc(addFilters = false)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    private static final AuthUser authUser = new AuthUser("user-1", "Test User", "tester");

    private OrderResponse createOrderResponse() {
        return new OrderResponse(1L, "ORD-001", "user-1", OrderStatus.PENDING,
                "주문 대기", List.of(), 1, 2, BigDecimal.valueOf(20000),
                BigDecimal.ZERO, BigDecimal.valueOf(20000), null, null,
                null, null, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    @DisplayName("should_createOrder_when_validRequest")
    void should_createOrder_when_validRequest() throws Exception {
        // given
        AddressRequest address = new AddressRequest("홍길동", "010-1234-5678",
                "12345", "서울시 강남구", "101호");
        CreateOrderRequest request = new CreateOrderRequest(address, null);
        OrderResponse response = createOrderResponse();
        when(orderService.createOrder(eq("user-1"), any(CreateOrderRequest.class))).thenReturn(response);

        // when/then
        mockMvc.perform(post("/orders")
                        .requestAttr("authUser", authUser)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderNumber").value("ORD-001"));
    }

    @Test
    @DisplayName("should_returnOrders_when_getUserOrders")
    void should_returnOrders_when_getUserOrders() throws Exception {
        // given
        OrderResponse response = createOrderResponse();
        Page<OrderResponse> page = new PageImpl<>(List.of(response), PageRequest.of(0, 10), 1);
        when(orderService.getUserOrders(eq("user-1"), any())).thenReturn(page);

        // when/then
        mockMvc.perform(get("/orders")
                        .requestAttr("authUser", authUser))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].orderNumber").value("ORD-001"));
    }

    @Test
    @DisplayName("should_returnOrder_when_getOrderByNumber")
    void should_returnOrder_when_getOrderByNumber() throws Exception {
        // given
        OrderResponse response = createOrderResponse();
        when(orderService.getOrder("user-1", "ORD-001")).thenReturn(response);

        // when/then
        mockMvc.perform(get("/orders/ORD-001")
                        .requestAttr("authUser", authUser))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderNumber").value("ORD-001"));
    }

    @Test
    @DisplayName("should_cancelOrder_when_validRequest")
    void should_cancelOrder_when_validRequest() throws Exception {
        // given
        CancelOrderRequest request = new CancelOrderRequest("단순 변심");
        OrderResponse response = createOrderResponse();
        when(orderService.cancelOrder(eq("user-1"), eq("ORD-001"), any(CancelOrderRequest.class)))
                .thenReturn(response);

        // when/then
        mockMvc.perform(post("/orders/ORD-001/cancel")
                        .requestAttr("authUser", authUser)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("should_returnBadRequest_when_cancelWithEmptyReason")
    void should_returnBadRequest_when_cancelWithEmptyReason() throws Exception {
        // when/then
        mockMvc.perform(post("/orders/ORD-001/cancel")
                        .requestAttr("authUser", authUser)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("should_returnBadRequest_when_createOrderWithInvalidAddress")
    void should_returnBadRequest_when_createOrderWithInvalidAddress() throws Exception {
        // when/then
        mockMvc.perform(post("/orders")
                        .requestAttr("authUser", authUser)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"shippingAddress\": null}"))
                .andExpect(status().isBadRequest());
    }
}
