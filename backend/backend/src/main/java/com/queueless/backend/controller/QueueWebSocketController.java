// Enhanced QueueWebSocketController with more real-time features
package com.queueless.backend.controller;

import com.queueless.backend.dto.ConnectRequest;
import com.queueless.backend.dto.ServeNextRequest;
import com.queueless.backend.enums.TokenStatus;
import com.queueless.backend.model.Queue;
import com.queueless.backend.model.QueueToken;
import com.queueless.backend.service.QueueService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

@Controller
@Slf4j
public class QueueWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final QueueService queueService;

    @Autowired
    public QueueWebSocketController(SimpMessagingTemplate messagingTemplate, QueueService queueService) {
        this.messagingTemplate = messagingTemplate;
        this.queueService = queueService;
    }

    @MessageMapping("/queue/connect")
    @PreAuthorize("hasAnyRole('USER', 'PROVIDER', 'ADMIN')")
    public void onConnect(@Payload ConnectRequest request, @Header("simpSessionId") String sessionId, Authentication authentication) {
        log.info("🎯 Client connected and requesting queue for ID: {} | User: {}", request.getQueueId(), authentication.getName());

        try {
            Queue queue = queueService.getQueueById(request.getQueueId());

            if (queue != null) {
                messagingTemplate.convertAndSendToUser(sessionId, "/topic/queues/" + request.getQueueId(), queue);
                log.info("✅ Sent initial queue state for ID: {}", request.getQueueId());
            } else {
                log.warn("⚠️ Queue not found for ID: {}", request.getQueueId());
                messagingTemplate.convertAndSendToUser(sessionId, "/topic/errors", "Queue not found");
            }
        } catch (Exception e) {
            log.error("Error fetching queue on connect: {}", e.getMessage());
            messagingTemplate.convertAndSendToUser(sessionId, "/topic/errors", "Error fetching queue: " + e.getMessage());
        }
    }

    @MessageMapping("/queue/serve-next")
    @PreAuthorize("hasAnyRole('PROVIDER', 'ADMIN')")
    public void serveNext(@Payload ServeNextRequest request, Authentication authentication) {
        log.info("🎯 Request to serve-next for queue: {} | User: {}", request.getQueueId(), authentication.getName());

        try {
            Queue updatedQueue = queueService.serveNextToken(request.getQueueId());

            if (updatedQueue != null) {
                // Find the token that is now 'in-service' for logging purposes
                Optional<QueueToken> inServiceToken = updatedQueue.getTokens().stream()
                        .filter(token -> TokenStatus.IN_SERVICE.toString().equals(token.getStatus()))
                        .findFirst();

                if (inServiceToken.isPresent()) {
                    log.info("✅ Served token {} for queue {}", inServiceToken.get().getTokenId(), updatedQueue.getId());
                } else {
                    log.info("⚠️ No tokens available for queue {}", updatedQueue.getId());
                }

                // Broadcast to all subscribers of this queue
                messagingTemplate.convertAndSend("/topic/queues/" + request.getQueueId(), updatedQueue);

                // Also send to the provider's private queue
                messagingTemplate.convertAndSendToUser(authentication.getName(), "/queue/provider-updates", updatedQueue);
            } else {
                log.warn("⚠️ Queue not found or no tokens to serve for ID: {}", request.getQueueId());
            }
        } catch (Exception e) {
            log.error("Error serving next token: {}", e.getMessage());
        }
    }

    @MessageMapping("/queue/add-token")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public void addToken(@Payload ConnectRequest request, Authentication authentication) {
        log.info("🎯 Request to add token to queue: {} | User: {}", request.getQueueId(), authentication.getName());

        try {
            // Get the token instead of trying to get a Queue
            QueueToken token = queueService.addNewToken(request.getQueueId(), authentication.getName());

            if (token != null) {
                // Fetch the updated queue to broadcast
                Queue updatedQueue = queueService.getQueueById(request.getQueueId());

                // Broadcast to all subscribers of this queue
                messagingTemplate.convertAndSend("/topic/queues/" + request.getQueueId(), updatedQueue);
                log.info("✅ Token added to queue {}", request.getQueueId());
            } else {
                log.warn("⚠️ Failed to add token to queue: {}", request.getQueueId());
            }
        } catch (Exception e) {
            log.error("Error adding token: {}", e.getMessage());
        }
    }
    @MessageMapping("/place/update")
    @PreAuthorize("hasRole('ADMIN')")
    public void onPlaceUpdate(@Payload String placeId, Authentication authentication) {
        log.info("🔄 Place update requested for ID: {} | User: {}", placeId, authentication.getName());

        // In a real implementation, you would fetch the updated place
        // and broadcast it to subscribed clients
        // messagingTemplate.convertAndSend("/topic/places/" + placeId, updatedPlace);

        messagingTemplate.convertAndSend("/topic/places/" + placeId, "Place updated: " + placeId);
    }

    @MessageMapping("/service/update")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROVIDER')")
    public void onServiceUpdate(@Payload String serviceId, Authentication authentication) {
        log.info("🔄 Service update requested for ID: {} | User: {}", serviceId, authentication.getName());

        // In a real implementation, you would fetch the updated service
        // and broadcast it to subscribed clients
        // messagingTemplate.convertAndSend("/topic/services/" + serviceId, updatedService);

        messagingTemplate.convertAndSend("/topic/services/" + serviceId, "Service updated: " + serviceId);
    }

    @MessageMapping("/queue/status")
    @PreAuthorize("hasAnyRole('PROVIDER', 'ADMIN')")
    public void toggleQueueStatus(@Payload String queueId, Authentication authentication) {
        log.info("🔄 Queue status toggle requested for ID: {} | User: {}", queueId, authentication.getName());

        try {
            Queue queue = queueService.getQueueById(queueId);
            if (queue != null) {
                Queue updatedQueue = queueService.setQueueActiveStatus(queueId, !queue.getIsActive());

                // Broadcast to all subscribers of this queue
                messagingTemplate.convertAndSend("/topic/queues/" + queueId, updatedQueue);

                // Also send to the general places topic for UI updates
                messagingTemplate.convertAndSend("/topic/places/" + queue.getPlaceId() + "/queues", updatedQueue);

                log.info("✅ Queue {} status changed to {}", queueId, updatedQueue.getIsActive() ? "ACTIVE" : "INACTIVE");
            }
        } catch (Exception e) {
            log.error("Error toggling queue status: {}", e.getMessage());
        }
    }
}