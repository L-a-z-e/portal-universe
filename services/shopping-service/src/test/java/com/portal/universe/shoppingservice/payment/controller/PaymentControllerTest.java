package com.portal.universe.shoppingservice.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.universe.commonlibrary.security.context.AuthUser;
import com.portal.universe.shoppingservice.payment.domain.PaymentMethod;
import com.portal.universe.shoppingservice.payment.domain.PaymentStatus;
import com.portal.universe.shoppingservice.payment.dto.PaymentResponse;
import com.portal.universe.shoppingservice.payment.dto.ProcessPaymentRequest;
import com.portal.universe.shoppingservice.payment.service.PaymentService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@ContextConfiguration(classes = {WebMvcTestConfig.class, PaymentController.class})
@AutoConfigureMockMvc(addFilters = false)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PaymentService paymentService;

    private static final AuthUser authUser = new AuthUser("user-1", "Test User", "tester");

    @BeforeEach
    void setUp() {
    }

    private PaymentResponse createPaymentResponse() {
        return new PaymentResponse(1L, "PAY-001", "ORD-001", "user-1",
                BigDecimal.valueOf(20000), PaymentStatus.COMPLETED, "결제 완료",
                PaymentMethod.CARD, "카드", "pg-tx-001", null,
                LocalDateTime.now(), null, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    @DisplayName("should_processPayment_when_validRequest")
    void should_processPayment_when_validRequest() throws Exception {
        // given
        ProcessPaymentRequest request = new ProcessPaymentRequest("ORD-001",
                PaymentMethod.CARD, "4111111111111111", "12/26", "123");
        PaymentResponse response = createPaymentResponse();
        when(paymentService.processPayment(eq("user-1"), any(ProcessPaymentRequest.class)))
                .thenReturn(response);

        // when/then
        mockMvc.perform(post("/payments").requestAttr("authUser", authUser)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.paymentNumber").value("PAY-001"));
    }

    @Test
    @DisplayName("should_returnPayment_when_getPayment")
    void should_returnPayment_when_getPayment() throws Exception {
        // given
        PaymentResponse response = createPaymentResponse();
        when(paymentService.getPayment("user-1", "PAY-001")).thenReturn(response);

        // when/then
        mockMvc.perform(get("/payments/PAY-001").requestAttr("authUser", authUser))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.paymentNumber").value("PAY-001"));
    }

    @Test
    @DisplayName("should_cancelPayment_when_called")
    void should_cancelPayment_when_called() throws Exception {
        // given
        PaymentResponse response = createPaymentResponse();
        when(paymentService.cancelPayment("user-1", "PAY-001")).thenReturn(response);

        // when/then
        mockMvc.perform(post("/payments/PAY-001/cancel").requestAttr("authUser", authUser))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("should_refundPayment_when_called")
    void should_refundPayment_when_called() throws Exception {
        // given
        PaymentResponse response = createPaymentResponse();
        when(paymentService.refundPayment("PAY-001")).thenReturn(response);

        // when/then
        mockMvc.perform(post("/payments/PAY-001/refund").requestAttr("authUser", authUser))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("should_returnBadRequest_when_invalidPaymentRequest")
    void should_returnBadRequest_when_invalidPaymentRequest() throws Exception {
        // when/then
        mockMvc.perform(post("/payments").requestAttr("authUser", authUser)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderNumber\": \"\"}"))
                .andExpect(status().isBadRequest());
    }
}
