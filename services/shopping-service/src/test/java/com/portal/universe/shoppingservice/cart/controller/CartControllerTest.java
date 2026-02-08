package com.portal.universe.shoppingservice.cart.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.universe.commonlibrary.security.context.AuthUser;
import com.portal.universe.shoppingservice.cart.domain.CartStatus;
import com.portal.universe.shoppingservice.cart.dto.AddCartItemRequest;
import com.portal.universe.shoppingservice.cart.dto.CartResponse;
import com.portal.universe.shoppingservice.cart.dto.UpdateCartItemRequest;
import com.portal.universe.shoppingservice.cart.service.CartService;
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

@WebMvcTest(CartController.class)
@ContextConfiguration(classes = {WebMvcTestConfig.class, CartController.class})
@AutoConfigureMockMvc(addFilters = false)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CartService cartService;

    private static final AuthUser authUser = new AuthUser("user-1", "Test User", "tester");

    private CartResponse createCartResponse() {
        return new CartResponse(1L, "user-1", CartStatus.ACTIVE, List.of(), 0, 0,
                BigDecimal.ZERO, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    @DisplayName("should_returnCart_when_getCart")
    void should_returnCart_when_getCart() throws Exception {
        // given
        CartResponse response = createCartResponse();
        when(cartService.getCart("user-1")).thenReturn(response);

        // when/then
        mockMvc.perform(get("/cart").requestAttr("authUser", authUser))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value("user-1"));
    }

    @Test
    @DisplayName("should_addItem_when_validRequest")
    void should_addItem_when_validRequest() throws Exception {
        // given
        AddCartItemRequest request = new AddCartItemRequest(1L, 2);
        CartResponse response = createCartResponse();
        when(cartService.addItem(eq("user-1"), any(AddCartItemRequest.class))).thenReturn(response);

        // when/then
        mockMvc.perform(post("/cart/items").requestAttr("authUser", authUser)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("should_returnBadRequest_when_addItemWithInvalidQuantity")
    void should_returnBadRequest_when_addItemWithInvalidQuantity() throws Exception {
        // when/then
        mockMvc.perform(post("/cart/items").requestAttr("authUser", authUser)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\": 1, \"quantity\": 0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("should_updateItemQuantity_when_validRequest")
    void should_updateItemQuantity_when_validRequest() throws Exception {
        // given
        UpdateCartItemRequest request = new UpdateCartItemRequest(5);
        CartResponse response = createCartResponse();
        when(cartService.updateItemQuantity(eq("user-1"), eq(1L), any(UpdateCartItemRequest.class)))
                .thenReturn(response);

        // when/then
        mockMvc.perform(put("/cart/items/1").requestAttr("authUser", authUser)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("should_removeItem_when_called")
    void should_removeItem_when_called() throws Exception {
        // given
        CartResponse response = createCartResponse();
        when(cartService.removeItem("user-1", 1L)).thenReturn(response);

        // when/then
        mockMvc.perform(delete("/cart/items/1").requestAttr("authUser", authUser))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("should_clearCart_when_called")
    void should_clearCart_when_called() throws Exception {
        // given
        CartResponse response = createCartResponse();
        when(cartService.clearCart("user-1")).thenReturn(response);

        // when/then
        mockMvc.perform(delete("/cart").requestAttr("authUser", authUser))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("should_checkout_when_called")
    void should_checkout_when_called() throws Exception {
        // given
        CartResponse response = createCartResponse();
        when(cartService.checkout("user-1")).thenReturn(response);

        // when/then
        mockMvc.perform(post("/cart/checkout").requestAttr("authUser", authUser))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("should_returnBadRequest_when_addItemWithNullProductId")
    void should_returnBadRequest_when_addItemWithNullProductId() throws Exception {
        // when/then
        mockMvc.perform(post("/cart/items").requestAttr("authUser", authUser)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": 1}"))
                .andExpect(status().isBadRequest());
    }
}
