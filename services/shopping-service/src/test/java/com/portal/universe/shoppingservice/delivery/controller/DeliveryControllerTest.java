package com.portal.universe.shoppingservice.delivery.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.universe.shoppingservice.delivery.domain.DeliveryStatus;
import com.portal.universe.shoppingservice.delivery.dto.DeliveryResponse;
import com.portal.universe.shoppingservice.delivery.dto.UpdateDeliveryStatusRequest;
import com.portal.universe.shoppingservice.delivery.service.DeliveryService;
import com.portal.universe.shoppingservice.support.WebMvcTestConfig;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
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

@WebMvcTest(DeliveryController.class)
@ContextConfiguration(classes = {WebMvcTestConfig.class, DeliveryController.class})
@AutoConfigureMockMvc(addFilters = false)
class DeliveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DeliveryService deliveryService;

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

    private DeliveryResponse createDeliveryResponse() {
        return new DeliveryResponse(1L, "TRK-001", "ORD-001",
                DeliveryStatus.PREPARING, "배송 준비중", "CJ대한통운",
                null, null, null, List.of(),
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    @DisplayName("should_returnDelivery_when_getByTrackingNumber")
    void should_returnDelivery_when_getByTrackingNumber() throws Exception {
        // given
        DeliveryResponse response = createDeliveryResponse();
        when(deliveryService.getDeliveryByTrackingNumber("TRK-001")).thenReturn(response);

        // when/then
        mockMvc.perform(get("/deliveries/TRK-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.trackingNumber").value("TRK-001"));
    }

    @Test
    @DisplayName("should_returnDelivery_when_getByOrderNumber")
    void should_returnDelivery_when_getByOrderNumber() throws Exception {
        // given
        DeliveryResponse response = createDeliveryResponse();
        when(deliveryService.getDeliveryByOrderNumber("ORD-001")).thenReturn(response);

        // when/then
        mockMvc.perform(get("/deliveries/order/ORD-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderNumber").value("ORD-001"));
    }

    @Test
    @DisplayName("should_updateDeliveryStatus_when_validRequest")
    void should_updateDeliveryStatus_when_validRequest() throws Exception {
        // given
        UpdateDeliveryStatusRequest request = new UpdateDeliveryStatusRequest(
                DeliveryStatus.IN_TRANSIT, "서울 물류센터", "배송 시작");
        DeliveryResponse response = createDeliveryResponse();
        when(deliveryService.updateDeliveryStatus(eq("TRK-001"), any(UpdateDeliveryStatusRequest.class)))
                .thenReturn(response);

        // when/then
        mockMvc.perform(put("/deliveries/TRK-001/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("should_returnBadRequest_when_statusIsNull")
    void should_returnBadRequest_when_statusIsNull() throws Exception {
        // when/then
        mockMvc.perform(put("/deliveries/TRK-001/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"location\": \"Seoul\"}"))
                .andExpect(status().isBadRequest());
    }
}
