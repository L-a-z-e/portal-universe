package com.portal.universe.shoppingservice.queue.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.universe.commonlibrary.security.context.AuthUser;
import com.portal.universe.shoppingservice.queue.domain.QueueStatus;
import com.portal.universe.shoppingservice.queue.dto.QueueStatusResponse;
import com.portal.universe.shoppingservice.queue.service.QueueService;
import com.portal.universe.shoppingservice.support.WebMvcTestConfig;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(QueueController.class)
@ContextConfiguration(classes = {WebMvcTestConfig.class, QueueController.class})
@AutoConfigureMockMvc(addFilters = false)
class QueueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private QueueService queueService;

    private static final AuthUser authUser = new AuthUser("user-1", "Test User", "tester", null);

    @Test
    @DisplayName("should_enterQueue_when_called")
    void should_enterQueue_when_called() throws Exception {
        // given
        QueueStatusResponse response = QueueStatusResponse.waiting("token-abc", 5L, 30L, 100L);
        when(queueService.enterQueue("TIMEDEAL", 1L, "user-1")).thenReturn(response);

        // when/then
        mockMvc.perform(post("/queue/TIMEDEAL/1/enter").requestAttr("authUser", authUser))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.entryToken").value("token-abc"))
                .andExpect(jsonPath("$.data.status").value("WAITING"))
                .andExpect(jsonPath("$.data.position").value(5));
    }

    @Test
    @DisplayName("should_returnQueueStatus_when_called")
    void should_returnQueueStatus_when_called() throws Exception {
        // given
        QueueStatusResponse response = QueueStatusResponse.waiting("token-abc", 3L, 20L, 50L);
        when(queueService.getQueueStatus("TIMEDEAL", 1L, "user-1")).thenReturn(response);

        // when/then
        mockMvc.perform(get("/queue/TIMEDEAL/1/status").requestAttr("authUser", authUser))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.position").value(3));
    }

    @Test
    @DisplayName("should_returnQueueStatusByToken_when_called")
    void should_returnQueueStatusByToken_when_called() throws Exception {
        // given
        QueueStatusResponse response = QueueStatusResponse.entered("token-abc");
        when(queueService.getQueueStatusByToken("token-abc")).thenReturn(response);

        // when/then
        mockMvc.perform(get("/queue/token/token-abc").requestAttr("authUser", authUser))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ENTERED"));
    }

    @Test
    @DisplayName("should_leaveQueue_when_called")
    void should_leaveQueue_when_called() throws Exception {
        // given
        doNothing().when(queueService).leaveQueue("TIMEDEAL", 1L, "user-1");

        // when/then
        mockMvc.perform(delete("/queue/TIMEDEAL/1/leave").requestAttr("authUser", authUser))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        verify(queueService).leaveQueue("TIMEDEAL", 1L, "user-1");
    }

    @Test
    @DisplayName("should_leaveQueueByToken_when_called")
    void should_leaveQueueByToken_when_called() throws Exception {
        // given
        doNothing().when(queueService).leaveQueueByToken("token-abc");

        // when/then
        mockMvc.perform(delete("/queue/token/token-abc").requestAttr("authUser", authUser))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        verify(queueService).leaveQueueByToken("token-abc");
    }

    @Test
    @DisplayName("should_returnWaitingResponse_when_enterQueueWithFullQueue")
    void should_returnWaitingResponse_when_enterQueueWithFullQueue() throws Exception {
        // given
        QueueStatusResponse response = QueueStatusResponse.waiting("token-xyz", 100L, 600L, 500L);
        when(queueService.enterQueue("COUPON", 2L, "user-1")).thenReturn(response);

        // when/then
        mockMvc.perform(post("/queue/COUPON/2/enter").requestAttr("authUser", authUser))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.position").value(100))
                .andExpect(jsonPath("$.data.totalWaiting").value(500));
    }
}
