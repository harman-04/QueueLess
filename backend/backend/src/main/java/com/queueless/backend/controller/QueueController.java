package com.queueless.backend.controller;

import com.queueless.backend.dto.TokenRequest;
import com.queueless.backend.exception.ResourceNotFoundException;
import com.queueless.backend.model.Queue;
import com.queueless.backend.model.QueueToken;
import com.queueless.backend.service.QueueService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/queues")
@RequiredArgsConstructor
public class QueueController {

    private static final Logger logger = LoggerFactory.getLogger(QueueController.class);
    private final QueueService queueService;

    public static class CreateQueueRequest {
        private String providerId;
        private String serviceName;

        public String getProviderId() { return providerId; }
        public void setProviderId(String providerId) { this.providerId = providerId; }
        public String getServiceName() { return serviceName; }
        public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    }

    @PostMapping("/create")
    public ResponseEntity<Queue> createNewQueue(@RequestBody CreateQueueRequest request) {
        logger.info("Creating new queue for providerId={}, serviceName={}", request.getProviderId(), request.getServiceName());
        Queue newQueue = queueService.createNewQueue(request.getProviderId(), request.getServiceName());
        logger.debug("Created queue: {}", newQueue);
        return new ResponseEntity<>(newQueue, HttpStatus.CREATED);
    }

    @GetMapping("/by-provider/{providerId}")
    public ResponseEntity<List<Queue>> getQueuesByProviderId(@PathVariable String providerId) {
        logger.info("Fetching queues for providerId={}", providerId);
        List<Queue> queues = queueService.getQueuesByProviderId(providerId);
        logger.debug("Found {} queues for providerId={}", queues.size(), providerId);
        return queues.isEmpty() ? new ResponseEntity<>(HttpStatus.NOT_FOUND) : new ResponseEntity<>(queues, HttpStatus.OK);
    }

    public static class AddTokenRequest {
        private String userId;
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
    }

    @PostMapping("/{queueId}/add-token")
    public ResponseEntity<QueueToken> addTokenToQueue(@PathVariable String queueId, @RequestBody AddTokenRequest request) {
        logger.info("Adding token to queueId={} for userId={}", queueId, request.getUserId());
        QueueToken newToken = queueService.addNewToken(queueId, request.getUserId());
        logger.debug("Token created: {}", newToken);
        return new ResponseEntity<>(newToken, HttpStatus.CREATED);
    }

    @PostMapping("/{queueId}/serve-next")
    public ResponseEntity<Queue> serveNextToken(@PathVariable String queueId) {
        logger.info("Serving next token for queueId={}", queueId);
        Queue updatedQueue = queueService.serveNextToken(queueId);
        logger.debug("Updated queue after serving: {}", updatedQueue);
        return updatedQueue != null ? new ResponseEntity<>(updatedQueue, HttpStatus.OK) : new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @GetMapping("/{queueId}")
    public ResponseEntity<Queue> getQueueById(@PathVariable String queueId) {
        logger.info("Fetching queue by ID: {}", queueId);
        Queue queue = queueService.getQueueById(queueId);
        if (queue != null) {
            logger.debug("Queue found: {}", queue);
            return new ResponseEntity<>(queue, HttpStatus.OK);
        } else {
            logger.warn("Queue not found with ID: {}", queueId);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/all")
    public ResponseEntity<List<Queue>> getAllQueues() {
        logger.info("Fetching all active queues for public users");
        List<Queue> queues = queueService.getAllQueues();
        logger.debug("Total active queues found: {}", queues.size());
        return new ResponseEntity<>(queues, HttpStatus.OK);
    }

    @PostMapping("/{queueId}/complete-token")
    public ResponseEntity<Queue> completeToken(@PathVariable String queueId, @RequestBody TokenRequest tokenRequest) {
        logger.info("Completing tokenId={} in queueId={}", tokenRequest.getTokenId(), queueId);
        try {
            Queue updatedQueue = queueService.completeToken(queueId, tokenRequest.getTokenId());
            logger.debug("Queue after completion: {}", updatedQueue);
            return new ResponseEntity<>(updatedQueue, HttpStatus.OK);
        } catch (ResourceNotFoundException e) {
            logger.error("Failed to complete token: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/{queueId}/cancel-token/{tokenId}")
    public ResponseEntity<Queue> cancelToken(@PathVariable String queueId, @PathVariable String tokenId) {
        logger.info("Cancelling tokenId={} from queueId={}", tokenId, queueId);
        Queue updatedQueue = queueService.cancelToken(queueId, tokenId);
        logger.debug("Queue after cancellation: {}", updatedQueue);
        return ResponseEntity.ok(updatedQueue);
    }

    @PutMapping("/{queueId}/reorder")
    public ResponseEntity<Queue> reorderTokens(@PathVariable String queueId, @RequestBody List<QueueToken> newTokens) {
        logger.info("Reordering tokens in queueId={}", queueId);
        try {
            Queue updatedQueue = queueService.reorderQueue(queueId, newTokens);
            logger.debug("Queue after reordering: {}", updatedQueue);
            return ResponseEntity.ok(updatedQueue);
        } catch (ResourceNotFoundException e) {
            logger.error("Failed to reorder tokens: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping("/{queueId}/activate")
    public ResponseEntity<Queue> activateQueue(@PathVariable String queueId) {
        logger.info("Activating queueId={}", queueId);
        Queue updatedQueue = queueService.setQueueActiveStatus(queueId, true);
        logger.debug("Queue activated: {}", updatedQueue);
        return ResponseEntity.ok(updatedQueue);
    }

    @PutMapping("/{queueId}/deactivate")
    public ResponseEntity<Queue> deactivateQueue(@PathVariable String queueId) {
        logger.info("Deactivating queueId={}", queueId);
        Queue updatedQueue = queueService.setQueueActiveStatus(queueId, false);
        logger.debug("Queue deactivated: {}", updatedQueue);
        return ResponseEntity.ok(updatedQueue);
    }

    // In QueueController.java
    @PostMapping("/cleanup-tokens")
    public ResponseEntity<String> cleanupTokenStatuses() {
        logger.info("Manual token cleanup triggered");
        queueService.cleanupInconsistentTokenStatuses();
        return ResponseEntity.ok("Token cleanup completed");
    }
}