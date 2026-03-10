package com.portal.universe.shoppingsettlementservice.settlement.controller;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.shoppingsettlementservice.common.exception.SettlementErrorCode;
import com.portal.universe.shoppingsettlementservice.settlement.dto.SettlementPeriodResponse;
import com.portal.universe.shoppingsettlementservice.settlement.dto.SettlementResponse;
import com.portal.universe.shoppingsettlementservice.settlement.service.SettlementService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SettlementController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("SettlementController")
class SettlementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SettlementService settlementService;

    @Nested
    @DisplayName("GET /periods")
    class GetPeriods {

        @Test
        @DisplayName("should_returnPeriodList_when_validRequest")
        void should_returnPeriodList_when_validRequest() throws Exception {
            SettlementPeriodResponse response = new SettlementPeriodResponse(
                    1L, "DAILY", LocalDate.of(2026, 2, 27), LocalDate.of(2026, 2, 27),
                    "COMPLETED", Instant.now());

            when(settlementService.getPeriods(eq("DAILY"), any())).thenReturn(List.of(response));

            mockMvc.perform(get("/periods").param("periodType", "DAILY"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].periodType").value("DAILY"));
        }

        @Test
        @DisplayName("should_useDefaultPeriodType_when_notSpecified")
        void should_useDefaultPeriodType_when_notSpecified() throws Exception {
            when(settlementService.getPeriods(eq("DAILY"), any())).thenReturn(List.of());

            mockMvc.perform(get("/periods"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray());
        }
    }

    @Nested
    @DisplayName("GET /periods/{periodId}")
    class GetPeriod {

        @Test
        @DisplayName("should_returnPeriod_when_exists")
        void should_returnPeriod_when_exists() throws Exception {
            SettlementPeriodResponse response = new SettlementPeriodResponse(
                    1L, "DAILY", LocalDate.of(2026, 2, 27), LocalDate.of(2026, 2, 27),
                    "PENDING", Instant.now());

            when(settlementService.getPeriod(1L)).thenReturn(response);

            mockMvc.perform(get("/periods/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.status").value("PENDING"));
        }

        @Test
        @DisplayName("should_return404_when_periodNotFound")
        void should_return404_when_periodNotFound() throws Exception {
            when(settlementService.getPeriod(999L))
                    .thenThrow(new CustomBusinessException(SettlementErrorCode.PERIOD_NOT_FOUND));

            mockMvc.perform(get("/periods/999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /sellers/{sellerId}")
    class GetSellerSettlements {

        @Test
        @DisplayName("should_returnPagedSettlements_when_sellerExists")
        void should_returnPagedSettlements_when_sellerExists() throws Exception {
            SettlementResponse response = new SettlementResponse(
                    1L, 1L, 100L, new BigDecimal("10000.00"), 5, BigDecimal.ZERO,
                    new BigDecimal("1000.00"), new BigDecimal("9000.00"),
                    "CALCULATED", null, Instant.now());

            Page<SettlementResponse> page = new PageImpl<>(List.of(response));
            when(settlementService.getSellerSettlements(eq(100L), any())).thenReturn(page);

            mockMvc.perform(get("/sellers/100"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].sellerId").value(100));
        }
    }

    @Nested
    @DisplayName("POST /periods/{periodId}/confirm")
    class ConfirmPeriod {

        @Test
        @DisplayName("should_returnSuccess_when_confirmPeriod")
        void should_returnSuccess_when_confirmPeriod() throws Exception {
            doNothing().when(settlementService).confirmPeriod(1L);

            mockMvc.perform(post("/periods/1/confirm"))
                    .andExpect(status().isOk());

            verify(settlementService).confirmPeriod(1L);
        }
    }

    @Nested
    @DisplayName("POST /periods/{periodId}/pay")
    class PayPeriod {

        @Test
        @DisplayName("should_returnSuccess_when_payPeriod")
        void should_returnSuccess_when_payPeriod() throws Exception {
            doNothing().when(settlementService).markPeriodPaid(1L);

            mockMvc.perform(post("/periods/1/pay"))
                    .andExpect(status().isOk());

            verify(settlementService).markPeriodPaid(1L);
        }

        @Test
        @DisplayName("should_return404_when_periodNotFoundOnPay")
        void should_return404_when_periodNotFoundOnPay() throws Exception {
            doThrow(new CustomBusinessException(SettlementErrorCode.PERIOD_NOT_FOUND))
                    .when(settlementService).markPeriodPaid(999L);

            mockMvc.perform(post("/periods/999/pay"))
                    .andExpect(status().isNotFound());
        }
    }
}
