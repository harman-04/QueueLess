package com.queueless.backend.service;

import com.queueless.backend.enums.TokenStatus;
import com.queueless.backend.exception.QueueInactiveException;
import com.queueless.backend.exception.ResourceNotFoundException;
import com.queueless.backend.model.Queue;
import com.queueless.backend.model.QueueToken;
import com.queueless.backend.repository.QueueRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QueueService {

    private static final Logger logger = LoggerFactory.getLogger(QueueService.class);

    private final QueueRepository queueRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /** 🔄 Utility: Fetch queue or throw */
    private Queue getQueueOrThrow(String queueId) {
        return queueRepository.findById(queueId)
                .orElseThrow(() -> {
                    logger.error("Queue not found with id {}", queueId);
                    return new ResourceNotFoundException("Queue not found with id " + queueId);
                });
    }

    /** 🆕 Create a new queue */
    public Queue createNewQueue(String providerId, String serviceName) {
        logger.info("Creating queue for providerId={}, serviceName={}", providerId, serviceName);
        Queue newQueue = new Queue(providerId, serviceName);
        return queueRepository.save(newQueue);
    }

    /** 📋 Get all queues for a provider */
    public List<Queue> getQueuesByProviderId(String providerId) {
        logger.info("Fetching queues for providerId={}", providerId);
        return queueRepository.findByProviderId(providerId);
    }

    /** 👥 User joins queue */
    public QueueToken addNewToken(String queueId, String userId) {
        Queue queue = getQueueOrThrow(queueId);

        if (!queue.isActive()) {
            logger.warn("Inactive queue join attempt: queueId={}", queueId);
            throw new QueueInactiveException("Provider is on break. Queue temporarily unavailable.");
        }

        int nextToken = queue.getTokenCounter() + 1;
        queue.setTokenCounter(nextToken);
        String tokenId = "T-" + String.format("%03d", nextToken);

        QueueToken token = new QueueToken(tokenId, userId, TokenStatus.WAITING.toString(), LocalDateTime.now());
        queue.getTokens().add(token);

        Queue updatedQueue = queueRepository.save(queue);
        broadcastQueueUpdate(queueId, updatedQueue);

        logger.debug("Token {} added to queueId={}", tokenId, queueId);
        return token;
    }

    /** ⏩ Serve next waiting token */
    // In QueueService.java - Update the serveNextToken method
    // In QueueService.java - Update the serveNextToken method
    public Queue serveNextToken(String queueId) {
        Queue queue = getQueueOrThrow(queueId);

        // First, complete any currently in-service token
        queue.getTokens().stream()
                .filter(t -> TokenStatus.IN_SERVICE.toString().equals(t.getStatus()))
                .findFirst()
                .ifPresent(inServiceToken -> {
                    inServiceToken.setStatus(TokenStatus.COMPLETED.toString());
                    logger.info("Completed previous in-service token: {}", inServiceToken.getTokenId());
                });

        // Then find the next WAITING token
        QueueToken nextToken = queue.getTokens().stream()
                .filter(t -> TokenStatus.WAITING.toString().equals(t.getStatus()))
                .findFirst()
                .orElse(null);

        if (nextToken != null) {
            nextToken.setStatus(TokenStatus.IN_SERVICE.toString());
            Queue updatedQueue = queueRepository.save(queue);
            broadcastQueueUpdate(queueId, updatedQueue);
            logger.info("Token {} moved to IN_SERVICE", nextToken.getTokenId());
            return updatedQueue;
        }

        logger.info("No waiting tokens in queueId={}", queueId);
        return queue;
    }
    /** 📌 Get queue by ID */
    public Queue getQueueById(String queueId) {
        return getQueueOrThrow(queueId);
    }

    /** 🟢 Get all active queues */
    public List<Queue> getAllQueues() {
        logger.info("Fetching all active queues");
        return queueRepository.findByIsActive(true);
    }

    /** ✅ Complete token */
    public Queue completeToken(String queueId, String tokenId) {
        Queue queue = getQueueOrThrow(queueId);

        QueueToken token = queue.getTokens().stream()
                .filter(t -> t.getTokenId().equals(tokenId))
                .findFirst()
                .orElseThrow(() -> {
                    logger.error("Token not found: {}", tokenId);
                    return new ResourceNotFoundException("Token not found with id " + tokenId);
                });

        token.setStatus(TokenStatus.COMPLETED.toString());
        Queue updatedQueue = queueRepository.save(queue);
        broadcastQueueUpdate(queueId, updatedQueue);

        logger.info("Token {} marked COMPLETED", tokenId);
        return updatedQueue;
    }

    /** ❌ Cancel token */
    public Queue cancelToken(String queueId, String tokenId) {
        Queue queue = getQueueOrThrow(queueId);

        boolean removed = queue.getTokens().removeIf(t -> t.getTokenId().equals(tokenId));
        if (!removed) {
            logger.error("Token not found for cancellation: {}", tokenId);
            throw new ResourceNotFoundException("Token not found with id " + tokenId);
        }

        Queue updatedQueue = queueRepository.save(queue);
        broadcastQueueUpdate(queueId, updatedQueue);

        logger.info("Token {} cancelled", tokenId);
        return updatedQueue;
    }

    /** 🔃 Reorder tokens */
    public Queue reorderQueue(String queueId, List<QueueToken> newTokens) {
        Queue queue = getQueueOrThrow(queueId);
        queue.setTokens(newTokens);
        Queue updatedQueue = queueRepository.save(queue);
        broadcastQueueUpdate(queueId, updatedQueue);

        logger.info("Queue reordered for queueId={}", queueId);
        return updatedQueue;
    }

    /** 🔁 Toggle queue active status */
    // In QueueService.java - Update the setQueueActiveStatus method
    public Queue setQueueActiveStatus(String queueId, boolean active) {
        Queue queue = getQueueOrThrow(queueId);
        queue.setActive(active); // This will call setIsActive due to Lombok
        Queue updatedQueue = queueRepository.save(queue);
        broadcastQueueUpdate(queueId, updatedQueue);
        logger.info("Queue {} active status changed to {}", queueId, active);
        return updatedQueue;
    }
    /** 📡 Broadcast helper */
    // In QueueService.java - Update the broadcastQueueUpdate method
    private void broadcastQueueUpdate(String queueId, Queue queue) {
        // Send to the specific queue topic
        messagingTemplate.convertAndSend("/topic/queues/" + queueId, queue);

        // Also send to a general topic for all queue updates
        messagingTemplate.convertAndSend("/topic/queues", queue);
    }


    // In QueueService.java - Add this method
    @PostConstruct
    public void cleanupInconsistentTokenStatuses() {
        logger.info("Cleaning up inconsistent token statuses...");
        List<Queue> allQueues = queueRepository.findAll();

        for (Queue queue : allQueues) {
            boolean needsUpdate = false;

            // Check for multiple IN_SERVICE tokens
            long inServiceCount = queue.getTokens().stream()
                    .filter(t -> TokenStatus.IN_SERVICE.toString().equals(t.getStatus()))
                    .count();

            if (inServiceCount > 1) {
                logger.warn("Queue {} has {} IN_SERVICE tokens, fixing...", queue.getId(), inServiceCount);

                // Keep only the first IN_SERVICE token, mark others as COMPLETED
                boolean foundFirst = false;
                for (QueueToken token : queue.getTokens()) {
                    if (TokenStatus.IN_SERVICE.toString().equals(token.getStatus())) {
                        if (!foundFirst) {
                            foundFirst = true;
                        } else {
                            token.setStatus(TokenStatus.COMPLETED.toString());
                            needsUpdate = true;
                        }
                    }
                }
            }

            if (needsUpdate) {
                queueRepository.save(queue);
                logger.info("Fixed inconsistent token statuses for queue {}", queue.getId());
            }
        }
    }
}