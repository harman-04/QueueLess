package com.queueless.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.queueless.backend.dto.FeedbackDTO;
import com.queueless.backend.model.Feedback;
import com.queueless.backend.model.Queue;
import com.queueless.backend.service.FeedbackService;
import com.queueless.backend.service.QueueService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.cache.type=none",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration"
})
@AutoConfigureMockMvc
class FeedbackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FeedbackService feedbackService;

    @MockitoBean
    private QueueService queueService;

    @Autowired
    private ObjectMapper objectMapper;

    private final String userId = "user123";
    private final String tokenId = "token123";
    private final String queueId = "queue123";
    private final String placeId = "place123";
    private final String providerId = "provider123";
    private final String feedbackId = "fb123";

    private Feedback createTestFeedback() {
        Feedback feedback = new Feedback();
        feedback.setId(feedbackId);
        feedback.setTokenId(tokenId);
        feedback.setQueueId(queueId);
        feedback.setUserId(userId);
        feedback.setProviderId(providerId);
        feedback.setPlaceId(placeId);
        feedback.setServiceId("service123");
        feedback.setRating(4);
        feedback.setComment("Great service");
        feedback.setStaffRating(5);
        feedback.setServiceRating(4);
        feedback.setWaitTimeRating(3);
        feedback.setCreatedAt(LocalDateTime.now());
        return feedback;
    }

    // ==================== SUBMIT FEEDBACK ====================

    @Test
    @WithMockUser(username = userId, roles = {"USER"})
    void submitFeedback_Success() throws Exception {
        FeedbackDTO request = new FeedbackDTO();
        request.setTokenId(tokenId);
        request.setQueueId(queueId);
        request.setRating(4);
        request.setComment("Great service");
        request.setStaffRating(5);
        request.setServiceRating(4);
        request.setWaitTimeRating(3);

        Queue queue = new Queue(providerId, "Service", placeId, "service123");
        queue.setId(queueId);

        Feedback savedFeedback = createTestFeedback();

        when(feedbackService.hasUserProvidedFeedbackForToken(userId, tokenId)).thenReturn(false);
        when(queueService.getQueueById(queueId)).thenReturn(queue);
        when(feedbackService.submitFeedback(any(Feedback.class))).thenReturn(savedFeedback);

        mockMvc.perform(post("/api/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(feedbackId))
                .andExpect(jsonPath("$.rating").value(4));
    }

    @Test
    @WithMockUser(username = userId, roles = {"USER"})
    void submitFeedback_AlreadyExists() throws Exception {
        FeedbackDTO request = new FeedbackDTO();
        request.setTokenId(tokenId);
        request.setQueueId(queueId);
        request.setRating(4);

        when(feedbackService.hasUserProvidedFeedbackForToken(userId, tokenId)).thenReturn(true);

        mockMvc.perform(post("/api/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = userId, roles = {"USER"})
    void submitFeedback_QueueNotFound() throws Exception {
        FeedbackDTO request = new FeedbackDTO();
        request.setTokenId(tokenId);
        request.setQueueId(queueId);
        request.setRating(4);

        when(feedbackService.hasUserProvidedFeedbackForToken(userId, tokenId)).thenReturn(false);
        when(queueService.getQueueById(queueId)).thenReturn(null);

        mockMvc.perform(post("/api/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = userId, roles = {"USER"})
    void submitFeedback_ServiceException() throws Exception {
        FeedbackDTO request = new FeedbackDTO();
        request.setTokenId(tokenId);
        request.setQueueId(queueId);
        request.setRating(4);

        when(feedbackService.hasUserProvidedFeedbackForToken(userId, tokenId)).thenReturn(false);
        when(queueService.getQueueById(queueId)).thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(post("/api/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }

    // ==================== GET FEEDBACK BY TOKEN ID ====================

    @Test
    @WithMockUser(username = userId, roles = {"USER"})
    void getFeedbackByTokenId_Owner_Success() throws Exception {
        Feedback feedback = createTestFeedback();
        when(feedbackService.getFeedbackByTokenId(tokenId)).thenReturn(Optional.of(feedback));

        mockMvc.perform(get("/api/feedback/token/{tokenId}", tokenId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(feedbackId));
    }

    @Test
    @WithMockUser(username = "otherUser", roles = {"USER"})
    void getFeedbackByTokenId_NotOwner_Forbidden() throws Exception {
        Feedback feedback = createTestFeedback(); // owned by userId
        when(feedbackService.getFeedbackByTokenId(tokenId)).thenReturn(Optional.of(feedback));

        mockMvc.perform(get("/api/feedback/token/{tokenId}", tokenId))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = providerId, roles = {"PROVIDER"})
    void getFeedbackByTokenId_Provider_Success() throws Exception {
        Feedback feedback = createTestFeedback(); // owned by userId but provider can see
        when(feedbackService.getFeedbackByTokenId(tokenId)).thenReturn(Optional.of(feedback));

        mockMvc.perform(get("/api/feedback/token/{tokenId}", tokenId))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getFeedbackByTokenId_Admin_Success() throws Exception {
        Feedback feedback = createTestFeedback();
        when(feedbackService.getFeedbackByTokenId(tokenId)).thenReturn(Optional.of(feedback));

        mockMvc.perform(get("/api/feedback/token/{tokenId}", tokenId))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = userId, roles = {"USER"})
    void getFeedbackByTokenId_NotFound() throws Exception {
        when(feedbackService.getFeedbackByTokenId(tokenId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/feedback/token/{tokenId}", tokenId))
                .andExpect(status().isNotFound());
    }

    // ==================== GET FEEDBACK BY PLACE ID ====================

    @Test
    void getFeedbackByPlaceId_Success() throws Exception {
        Feedback feedback = createTestFeedback();
        when(feedbackService.getFeedbackByPlaceId(placeId)).thenReturn(List.of(feedback));

        mockMvc.perform(get("/api/feedback/place/{placeId}", placeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(feedbackId));
    }

    // ==================== GET FEEDBACK BY PROVIDER ID ====================

    @Test
    @WithMockUser(username = providerId, roles = {"PROVIDER"})
    void getFeedbackByProviderId_Own_Success() throws Exception {
        Feedback feedback = createTestFeedback();
        when(feedbackService.getFeedbackByProviderId(providerId)).thenReturn(List.of(feedback));

        mockMvc.perform(get("/api/feedback/provider/{providerId}", providerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(feedbackId));
    }

    @Test
    @WithMockUser(username = "otherProvider", roles = {"PROVIDER"})
    void getFeedbackByProviderId_OtherProvider_Forbidden() throws Exception {
        mockMvc.perform(get("/api/feedback/provider/{providerId}", providerId))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getFeedbackByProviderId_Admin_AccessAny() throws Exception {
        Feedback feedback = createTestFeedback();
        when(feedbackService.getFeedbackByProviderId(providerId)).thenReturn(List.of(feedback));

        mockMvc.perform(get("/api/feedback/provider/{providerId}", providerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(feedbackId));
    }

    // ==================== AVERAGE RATING FOR PLACE ====================

    @Test
    void getAverageRatingForPlace_Success() throws Exception {
        when(feedbackService.getAverageRatingForPlace(placeId)).thenReturn(4.5);

        mockMvc.perform(get("/api/feedback/place/{placeId}/average-rating", placeId))
                .andExpect(status().isOk())
                .andExpect(content().string("4.5"));
    }

    @Test
    void getAverageRatingForPlace_Error() throws Exception {
        when(feedbackService.getAverageRatingForPlace(placeId)).thenThrow(new RuntimeException("Error"));

        mockMvc.perform(get("/api/feedback/place/{placeId}/average-rating", placeId))
                .andExpect(status().isOk())
                .andExpect(content().string("0.0"));
    }

    // ==================== AVERAGE RATING FOR PROVIDER ====================

    @Test
    void getAverageRatingForProvider_Success() throws Exception {
        when(feedbackService.getAverageRatingForProvider(providerId)).thenReturn(4.2);

        mockMvc.perform(get("/api/feedback/provider/{providerId}/average-rating", providerId))
                .andExpect(status().isOk())
                .andExpect(content().string("4.2"));
    }

    @Test
    void getAverageRatingForProvider_Error() throws Exception {
        when(feedbackService.getAverageRatingForProvider(providerId)).thenThrow(new RuntimeException("Error"));

        mockMvc.perform(get("/api/feedback/provider/{providerId}/average-rating", providerId))
                .andExpect(status().isOk())
                .andExpect(content().string("0.0"));
    }

    // ==================== HAS USER PROVIDED FEEDBACK ====================

    @Test
    @WithMockUser(username = userId, roles = {"USER"})
    void hasUserProvidedFeedback_Success() throws Exception {
        when(feedbackService.hasUserProvidedFeedbackForToken(userId, tokenId)).thenReturn(true);

        mockMvc.perform(get("/api/feedback/user/{userId}/token/{tokenId}", userId, tokenId))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    @WithMockUser(username = "otherUser", roles = {"USER"})
    void hasUserProvidedFeedback_Forbidden() throws Exception {
        mockMvc.perform(get("/api/feedback/user/{userId}/token/{tokenId}", userId, tokenId))
                .andExpect(status().isForbidden());
    }

    // ==================== DETAILED RATINGS FOR PLACE ====================

    @Test
    void getDetailedRatingsForPlace_Success() throws Exception {
        Map<String, Double> ratings = Map.of(
                "overall", 4.0,
                "staff", 4.5,
                "service", 3.8,
                "waitTime", 3.2
        );
        when(feedbackService.getAllAverageRatingsForPlace(placeId)).thenReturn(ratings);

        mockMvc.perform(get("/api/feedback/place/{placeId}/detailed-ratings", placeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overall").value(4.0))
                .andExpect(jsonPath("$.staff").value(4.5))
                .andExpect(jsonPath("$.service").value(3.8))
                .andExpect(jsonPath("$.waitTime").value(3.2));
    }

    @Test
    void getDetailedRatingsForPlace_Error() throws Exception {
        when(feedbackService.getAllAverageRatingsForPlace(placeId)).thenThrow(new RuntimeException("Error"));

        mockMvc.perform(get("/api/feedback/place/{placeId}/detailed-ratings", placeId))
                .andExpect(status().isInternalServerError());
    }

    // ==================== RECENT FEEDBACK ====================

    @Test
    void getRecentFeedback_Success() throws Exception {
        Feedback feedback = createTestFeedback();
        when(feedbackService.getRecentFeedback(5)).thenReturn(List.of(feedback));

        mockMvc.perform(get("/api/feedback/recent")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tokenId").value(tokenId))
                .andExpect(jsonPath("$[0].rating").value(4));
    }
}