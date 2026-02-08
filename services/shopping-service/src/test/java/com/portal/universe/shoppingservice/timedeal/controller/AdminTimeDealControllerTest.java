package com.portal.universe.shoppingservice.timedeal.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.universe.shoppingservice.timedeal.domain.TimeDealStatus;
import com.portal.universe.shoppingservice.timedeal.dto.TimeDealCreateRequest;
import com.portal.universe.shoppingservice.timedeal.dto.TimeDealResponse;
import com.portal.universe.shoppingservice.timedeal.service.TimeDealService;
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

@WebMvcTest(AdminTimeDealController.class)
@ContextConfiguration(classes = {WebMvcTestConfig.class, AdminTimeDealController.class})
@AutoConfigureMockMvc(addFilters = false)
class AdminTimeDealControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TimeDealService timeDealService;

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

    @Test
    @DisplayName("should_returnTimeDeals_when_getTimeDeals")
    void should_returnTimeDeals_when_getTimeDeals() throws Exception {
        // given
        Page<TimeDealResponse> page = new PageImpl<>(
                List.of(createTimeDealResponse()), PageRequest.of(0, 10), 1);
        when(timeDealService.getAllTimeDeals(any())).thenReturn(page);

        // when/then
        mockMvc.perform(get("/admin/time-deals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].name").value("Flash Sale"));
    }

    @Test
    @DisplayName("should_createTimeDeal_when_validRequest")
    void should_createTimeDeal_when_validRequest() throws Exception {
        // given
        TimeDealCreateRequest.TimeDealProductRequest productReq = TimeDealCreateRequest.TimeDealProductRequest.builder()
                .productId(1L)
                .dealPrice(BigDecimal.valueOf(5000))
                .dealQuantity(100)
                .maxPerUser(5)
                .build();
        TimeDealCreateRequest request = TimeDealCreateRequest.builder()
                .name("New Flash Sale")
                .description("Limited offer")
                .startsAt(LocalDateTime.now())
                .endsAt(LocalDateTime.now().plusDays(1))
                .products(List.of(productReq))
                .build();
        when(timeDealService.createTimeDeal(any(TimeDealCreateRequest.class))).thenReturn(createTimeDealResponse());

        // when/then
        mockMvc.perform(post("/admin/time-deals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("should_returnTimeDeal_when_getById")
    void should_returnTimeDeal_when_getById() throws Exception {
        // given
        when(timeDealService.getTimeDeal(1L)).thenReturn(createTimeDealResponse());

        // when/then
        mockMvc.perform(get("/admin/time-deals/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("should_cancelTimeDeal_when_called")
    void should_cancelTimeDeal_when_called() throws Exception {
        // given
        doNothing().when(timeDealService).cancelTimeDeal(1L);

        // when/then
        mockMvc.perform(delete("/admin/time-deals/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        verify(timeDealService).cancelTimeDeal(1L);
    }

    @Test
    @DisplayName("should_returnBadRequest_when_invalidCreateRequest")
    void should_returnBadRequest_when_invalidCreateRequest() throws Exception {
        // when/then
        mockMvc.perform(post("/admin/time-deals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
