package com.queueless.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.queueless.backend.config.RateLimitConfig;
import com.queueless.backend.config.TestSecurityConfig;
import com.queueless.backend.dto.*;
import com.queueless.backend.exception.QueueInactiveException;
import com.queueless.backend.exception.UserAlreadyInQueueException;
import com.queueless.backend.model.Queue;
import com.queueless.backend.model.QueueToken;
import com.queueless.backend.service.QRCodeService;
import com.queueless.backend.service.QueueService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = QueueController.class,
        properties = "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration")
@Import({RateLimitConfig.class, TestSecurityConfig.class})
@AutoConfigureMockMvc
class QueueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QueueService queueService;

    @MockitoBean
    private QRCodeService qrCodeService;

    @Autowired
    private ObjectMapper objectMapper;

    private final String providerId = "provider123";
    private final String queueId = "queue123";
    private final String userId = "user123";

    // ==================== CREATE QUEUE ====================

    @Test
    @WithMockUser(username = providerId, roles = {"PROVIDER"})
    void createNewQueue_Success() throws Exception {
        CreateQueueRequest request = new CreateQueueRequest();
        request.setServiceName("Test Queue");
        request.setPlaceId("place123");
        request.setServiceId("service123");
        request.setMaxCapacity(10);
        request.setSupportsGroupToken(true);
        request.setEmergencySupport(false);
        request.setEmergencyPriorityWeight(5);
        request.setRequiresEmergencyApproval(false);
        request.setAutoApproveEmergency(true);

        Queue queue = new Queue(providerId, "Test Queue", "place123", "service123");
        queue.setId(queueId);

        when(queueService.createNewQueue(
                eq(providerId),
                eq("Test Queue"),
                eq("place123"),
                eq("service123"),
                eq(10),
                eq(true),
                eq(false),
                eq(5),
                eq(false),
                eq(true)
        )).thenReturn(queue);

        mockMvc.perform(post("/api/queues/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(queueId))
                .andExpect(jsonPath("$.serviceName").value("Test Queue"));
    }

    @Test
    @WithMockUser(username = providerId, roles = {"PROVIDER"})
    void createNewQueue_Exception() throws Exception {
        CreateQueueRequest request = new CreateQueueRequest();
        request.setServiceName("Test Queue");
        request.setPlaceId("place123");
        request.setServiceId("service123");

        when(queueService.createNewQueue(anyString(), anyString(), anyString(), anyString(),
                any(), any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Service error"));

        mockMvc.perform(post("/api/queues/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }

    // ==================== GET QUEUES BY PROVIDER ====================

    @Test
    @WithMockUser(username = providerId, roles = {"PROVIDER"})
    void getQueuesByProviderId_Success() throws Exception {
        Queue queue = new Queue(providerId, "Queue 1", "place123", "service123");
        queue.setId(queueId);

        when(queueService.getQueuesByProviderId(providerId)).thenReturn(List.of(queue));

        mockMvc.perform(get("/api/queues/by-provider"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(queueId));
    }

    @Test
    @WithMockUser(username = providerId, roles = {"PROVIDER"})
    void getQueuesByProviderId_NoContent() throws Exception {
        when(queueService.getQueuesByProviderId(providerId)).thenReturn(List.of());

        mockMvc.perform(get("/api/queues/by-provider"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = providerId, roles = {"PROVIDER"})
    void getQueuesByProviderId_Exception() throws Exception {
        when(queueService.getQueuesByProviderId(providerId)).thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(get("/api/queues/by-provider"))
                .andExpect(status().isInternalServerError());
    }

    // ==================== GET QUEUES BY PLACE ====================

    @Test
    void getQueuesByPlaceId_Success() throws Exception {
        Queue queue = new Queue(providerId, "Queue 1", "place123", "service123");
        queue.setId(queueId);

        when(queueService.getQueuesByPlaceId("place123")).thenReturn(List.of(queue));

        mockMvc.perform(get("/api/queues/by-place/place123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(queueId));
    }

    @Test
    void getQueuesByPlaceId_NotFound() throws Exception {
        when(queueService.getQueuesByPlaceId("place123")).thenReturn(List.of());

        mockMvc.perform(get("/api/queues/by-place/place123"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getQueuesByPlaceId_Exception() throws Exception {
        when(queueService.getQueuesByPlaceId("place123")).thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(get("/api/queues/by-place/place123"))
                .andExpect(status().isInternalServerError());
    }

    // ==================== ADD TOKEN ====================

    @Test
    @WithMockUser(username = userId, roles = {"USER"})
    void addTokenToQueue_Success() throws Exception {
        QueueToken token = new QueueToken("T-001", userId, "WAITING", LocalDateTime.now());

        when(queueService.addNewToken(queueId, userId)).thenReturn(token);

        mockMvc.perform(post("/api/queues/{queueId}/add-token", queueId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tokenId").value("T-001"));
    }

    @Test
    @WithMockUser(username = userId, roles = {"USER"})
    void addTokenToQueue_UserAlreadyInQueue() throws Exception {
        when(queueService.addNewToken(queueId, userId))
                .thenThrow(new UserAlreadyInQueueException("User already in queue"));

        mockMvc.perform(post("/api/queues/{queueId}/add-token", queueId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("User already in queue"));
    }

    @Test
    @WithMockUser(username = userId, roles = {"USER"})
    void addTokenToQueue_QueueInactive() throws Exception {
        when(queueService.addNewToken(queueId, userId))
                .thenThrow(new QueueInactiveException("Queue is paused"));

        mockMvc.perform(post("/api/queues/{queueId}/add-token", queueId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Queue is paused"));
    }

    // ==================== SERVE NEXT ====================

    @Test
    @WithMockUser(username = providerId, roles = {"PROVIDER"})
    void serveNextToken_Success() throws Exception {
        Queue queue = new Queue(providerId, "Queue", "place123", "service123");
        queue.setId(queueId);

        when(queueService.getQueueById(queueId)).thenReturn(queue);
        when(queueService.serveNextToken(queueId)).thenReturn(queue);

        mockMvc.perform(post("/api/queues/{queueId}/serve-next", queueId))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "otherProvider", roles = {"PROVIDER"})
    void serveNextToken_Forbidden() throws Exception {
        Queue queue = new Queue(providerId, "Queue", "place123", "service123");
        queue.setId(queueId);

        when(queueService.getQueueById(queueId)).thenReturn(queue);

        mockMvc.perform(post("/api/queues/{queueId}/serve-next", queueId))
                .andExpect(status().isForbidden());
    }

    // ==================== COMPLETE TOKEN ====================

    @Test
    @WithMockUser(username = providerId, roles = {"PROVIDER"})
    void completeToken_Success() throws Exception {
        Queue queue = new Queue(providerId, "Queue", "place123", "service123");
        queue.setId(queueId);

        TokenRequest tokenRequest = new TokenRequest();
        tokenRequest.setTokenId("T-001");

        when(queueService.getQueueById(queueId)).thenReturn(queue);
        when(queueService.completeToken(queueId, "T-001")).thenReturn(queue);

        mockMvc.perform(post("/api/queues/{queueId}/complete-token", queueId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tokenRequest)))
                .andExpect(status().isOk());
    }

    // ==================== CANCEL TOKEN ====================

    @Test
    @WithMockUser(username = userId, roles = {"USER"})
    void cancelToken_UserCancelsOwnToken() throws Exception {
        Queue queue = new Queue(providerId, "Queue", "place123", "service123");
        queue.setId(queueId);

        QueueToken token = new QueueToken("T-001", userId, "WAITING", LocalDateTime.now());
        queue.setTokens(List.of(token));

        when(queueService.getQueueById(queueId)).thenReturn(queue);
        when(queueService.cancelToken(eq(queueId), eq("T-001"), any()))
                .thenReturn(queue);

        mockMvc.perform(delete("/api/queues/{queueId}/cancel-token/{tokenId}", queueId, "T-001"))
                .andExpect(status().isOk());
    }
}