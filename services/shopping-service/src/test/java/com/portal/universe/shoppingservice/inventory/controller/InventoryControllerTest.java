package com.portal.universe.shoppingservice.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.universe.shoppingservice.inventory.dto.InventoryBatchRequest;
import com.portal.universe.shoppingservice.inventory.dto.InventoryResponse;
import com.portal.universe.shoppingservice.inventory.dto.InventoryUpdateRequest;
import com.portal.universe.shoppingservice.inventory.dto.StockMovementResponse;
import com.portal.universe.shoppingservice.inventory.service.InventoryService;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InventoryController.class)
@ContextConfiguration(classes = {WebMvcTestConfig.class, InventoryController.class})
@AutoConfigureMockMvc(addFilters = false)
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InventoryService inventoryService;

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

    private InventoryResponse createInventoryResponse(Long productId) {
        return new InventoryResponse(1L, productId, 100, 10, 110,
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    @DisplayName("should_returnInventories_when_batchRequest")
    void should_returnInventories_when_batchRequest() throws Exception {
        // given
        InventoryBatchRequest request = new InventoryBatchRequest(List.of(1L, 2L));
        List<InventoryResponse> responses = List.of(
                createInventoryResponse(1L), createInventoryResponse(2L));
        when(inventoryService.getInventories(List.of(1L, 2L))).thenReturn(responses);

        // when/then
        mockMvc.perform(post("/inventory/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @DisplayName("should_returnInventory_when_getByProductId")
    void should_returnInventory_when_getByProductId() throws Exception {
        // given
        InventoryResponse response = createInventoryResponse(1L);
        when(inventoryService.getInventory(1L)).thenReturn(response);

        // when/then
        mockMvc.perform(get("/inventory/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.productId").value(1));
    }

    @Test
    @DisplayName("should_initializeInventory_when_validRequest")
    void should_initializeInventory_when_validRequest() throws Exception {
        // given
        InventoryUpdateRequest request = new InventoryUpdateRequest(100, "Initial stock");
        InventoryResponse response = createInventoryResponse(1L);
        when(inventoryService.initializeInventory(eq(1L), eq(100), eq("user-1")))
                .thenReturn(response);

        // when/then
        mockMvc.perform(post("/inventory/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("should_addStock_when_validRequest")
    void should_addStock_when_validRequest() throws Exception {
        // given
        InventoryUpdateRequest request = new InventoryUpdateRequest(50, "Restock");
        InventoryResponse response = createInventoryResponse(1L);
        when(inventoryService.addStock(eq(1L), eq(50), eq("Restock"), eq("user-1")))
                .thenReturn(response);

        // when/then
        mockMvc.perform(put("/inventory/1/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("should_returnStockMovements_when_called")
    void should_returnStockMovements_when_called() throws Exception {
        // given
        Page<StockMovementResponse> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(inventoryService.getStockMovements(eq(1L), any())).thenReturn(page);

        // when/then
        mockMvc.perform(get("/inventory/1/movements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("should_returnBadRequest_when_batchRequestEmpty")
    void should_returnBadRequest_when_batchRequestEmpty() throws Exception {
        // when/then
        mockMvc.perform(post("/inventory/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productIds\": []}"))
                .andExpect(status().isBadRequest());
    }
}
