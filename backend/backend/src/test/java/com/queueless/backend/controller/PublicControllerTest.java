package com.queueless.backend.controller;

import com.queueless.backend.config.RateLimitConfig;
import com.queueless.backend.config.TestSecurityConfig;
import com.queueless.backend.model.Queue;
import com.queueless.backend.model.QueueToken;
import com.queueless.backend.repository.PlaceRepository;
import com.queueless.backend.repository.QueueRepository;
import com.queueless.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PublicController.class,
        properties = "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration")
@Import({RateLimitConfig.class, TestSecurityConfig.class})
@AutoConfigureMockMvc
class PublicControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private PlaceRepository placeRepository;

    @MockitoBean
    private QueueRepository queueRepository;

    @Test
    void getPublicStats_Success() throws Exception {
        when(userRepository.count()).thenReturn(100L);
        when(placeRepository.count()).thenReturn(50L);

        Queue queue = new Queue("provider", "Service", "place", "service");
        QueueToken token1 = new QueueToken("T1", "user1", "COMPLETED", LocalDateTime.now());
        QueueToken token2 = new QueueToken("T2", "user2", "WAITING", LocalDateTime.now());
        QueueToken token3 = new QueueToken("T3", "user3", "COMPLETED", LocalDateTime.now());
        queue.setTokens(List.of(token1, token2, token3));

        when(queueRepository.findAll()).thenReturn(List.of(queue));

        mockMvc.perform(get("/api/public/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(100))
                .andExpect(jsonPath("$.totalPlaces").value(50))
                .andExpect(jsonPath("$.totalQueuesServed").value(2));
    }

    @Test
    void getLiveStats_Success() throws Exception {
        Queue activeQueue1 = new Queue("provider", "Service1", "place", "service");
        activeQueue1.setIsActive(true);
        activeQueue1.setEstimatedWaitTime(10);

        Queue activeQueue2 = new Queue("provider", "Service2", "place", "service");
        activeQueue2.setIsActive(true);
        activeQueue2.setEstimatedWaitTime(20);

        Queue inactiveQueue = new Queue("provider", "Service3", "place", "service");
        inactiveQueue.setIsActive(false);
        inactiveQueue.setEstimatedWaitTime(5);

        when(queueRepository.findAll()).thenReturn(List.of(activeQueue1, activeQueue2, inactiveQueue));

        mockMvc.perform(get("/api/public/live-stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeQueues").value(2))
                .andExpect(jsonPath("$.averageWaitTime").value(15.0));
    }

    @Test
    void getLiveStats_NoActiveQueues() throws Exception {
        Queue inactiveQueue = new Queue("provider", "Service", "place", "service");
        inactiveQueue.setIsActive(false);
        when(queueRepository.findAll()).thenReturn(List.of(inactiveQueue));

        mockMvc.perform(get("/api/public/live-stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeQueues").value(0))
                .andExpect(jsonPath("$.averageWaitTime").value(0.0));
    }
}