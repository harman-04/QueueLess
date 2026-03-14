package com.queueless.backend.controller;

import com.queueless.backend.dto.ConnectRequest;
import com.queueless.backend.dto.ServeNextRequest;
import com.queueless.backend.model.Queue;
import com.queueless.backend.model.QueueToken;
import com.queueless.backend.service.QueueService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QueueWebSocketControllerUnitTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private QueueService queueService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private QueueWebSocketController controller;

    @Test
    void onConnect_Success() {
        String queueId = "queue123";
        String sessionId = "sess123";
        ConnectRequest request = new ConnectRequest(queueId);
        Queue queue = new Queue("provider", "Service", "place", "service");
        queue.setId(queueId);

        when(authentication.getName()).thenReturn("user");
        when(queueService.getQueueById(queueId)).thenReturn(queue);

        controller.onConnect(request, sessionId, authentication);

        verify(messagingTemplate).convertAndSendToUser(eq(sessionId), eq("/topic/queues/" + queueId), eq(queue));
        verifyNoMoreInteractions(messagingTemplate);
    }

    @Test
    void onConnect_QueueNotFound() {
        String queueId = "queue123";
        String sessionId = "sess123";
        ConnectRequest request = new ConnectRequest(queueId);

        when(authentication.getName()).thenReturn("user");
        when(queueService.getQueueById(queueId)).thenReturn(null);

        controller.onConnect(request, sessionId, authentication);

        verify(messagingTemplate).convertAndSendToUser(eq(sessionId), eq("/topic/errors"), eq("Queue not found"));
    }

    @Test
    void serveNext_Success() {
        String queueId = "queue123";
        ServeNextRequest request = new ServeNextRequest(queueId);
        Queue queue = new Queue("provider", "Service", "place", "service");
        queue.setId(queueId);
        QueueToken token = new QueueToken("T1", "user", "WAITING", LocalDateTime.now());
        queue.setTokens(List.of(token));

        when(authentication.getName()).thenReturn("provider");
        when(queueService.serveNextToken(queueId)).thenReturn(queue);

        controller.serveNext(request, authentication);

        verify(messagingTemplate).convertAndSend(eq("/topic/queues/" + queueId), eq(queue));
        verify(messagingTemplate).convertAndSendToUser(eq("provider"), eq("/queue/provider-updates"), eq(queue));
    }

    @Test
    void addToken_Success() {
        String queueId = "queue123";
        ConnectRequest request = new ConnectRequest(queueId);
        QueueToken token = new QueueToken("T1", "user", "WAITING", LocalDateTime.now());
        Queue updatedQueue = new Queue("provider", "Service", "place", "service");
        updatedQueue.setId(queueId);

        when(authentication.getName()).thenReturn("user");
        when(queueService.addNewToken(queueId, "user")).thenReturn(token);
        when(queueService.getQueueById(queueId)).thenReturn(updatedQueue);

        controller.addToken(request, authentication);

        verify(messagingTemplate).convertAndSend(eq("/topic/queues/" + queueId), eq(updatedQueue));
    }

    @Test
    void toggleQueueStatus_Success() {
        String queueId = "queue123";
        String payload = queueId;
        Queue queue = new Queue("provider", "Service", "place", "service");
        queue.setId(queueId);
        queue.setIsActive(true);

        when(authentication.getName()).thenReturn("provider");
        when(queueService.getQueueById(queueId)).thenReturn(queue);
        when(queueService.setQueueActiveStatus(queueId, false)).thenReturn(queue);

        controller.toggleQueueStatus(payload, authentication);

        verify(messagingTemplate).convertAndSend(eq("/topic/queues/" + queueId), eq(queue));
        verify(messagingTemplate).convertAndSend(eq("/topic/places/" + queue.getPlaceId() + "/queues"), eq(queue));
    }
}