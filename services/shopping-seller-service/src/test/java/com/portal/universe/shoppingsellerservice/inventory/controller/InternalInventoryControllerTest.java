package com.portal.universe.shoppingsellerservice.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.universe.shoppingsellerservice.inventory.dto.StockReserveRequest;
import com.portal.universe.shoppingsellerservice.inventory.service.InventoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InternalInventoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("InternalInventoryController")
class InternalInventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InventoryService inventoryService;

    private String createRequestJson() throws Exception {
        return objectMapper.writeValueAsString(new StockReserveRequest("ORD-001", Map.of(1L, 10)));
    }

    @Test
    @DisplayName("POST /internal/inventory/reserve should call reserveStock")
    void should_reserve_stock() throws Exception {
        mockMvc.perform(post("/internal/inventory/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(inventoryService).reserveStock(any(StockReserveRequest.class));
    }

    @Test
    @DisplayName("POST /internal/inventory/deduct should call deductStock")
    void should_deduct_stock() throws Exception {
        mockMvc.perform(post("/internal/inventory/deduct")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(inventoryService).deductStock(any(StockReserveRequest.class));
    }

    @Test
    @DisplayName("POST /internal/inventory/release should call releaseStock")
    void should_release_stock() throws Exception {
        mockMvc.perform(post("/internal/inventory/release")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(inventoryService).releaseStock(any(StockReserveRequest.class));
    }

    @Test
    @DisplayName("POST /internal/inventory/restore should call restoreStock")
    void should_restore_stock() throws Exception {
        mockMvc.perform(post("/internal/inventory/restore")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(inventoryService).restoreStock(any(StockReserveRequest.class));
    }
}
