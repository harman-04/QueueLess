package com.queueless.backend.service;

import com.queueless.backend.enums.TokenStatus;
import com.queueless.backend.exception.QueueInactiveException;
import com.queueless.backend.exception.ResourceNotFoundException;
import com.queueless.backend.exception.UserAlreadyInQueueException;
import com.queueless.backend.model.Queue;
import com.queueless.backend.model.QueueToken;
import com.queueless.backend.repository.QueueRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
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
                    log.error("Queue not found with id {}", queueId);
                    return new ResourceNotFoundException("Queue not found with id " + queueId);
                });
    }

    /** 👥 User joins queue */
    public QueueToken addNewToken(String queueId, String userId) {
        Queue queue = getQueueOrThrow(queueId);

        if (!queue.getIsActive()) {
            log.warn("Inactive queue join attempt: queueId={}", queueId);
            throw new QueueInactiveException("Provider is on break. Queue temporarily unavailable.");
        }

        // Check if user already has an active token in this queue
        boolean hasActiveToken = queue.getTokens().stream()
                .anyMatch(token -> token.getUserId().equals(userId) &&
                        (TokenStatus.WAITING.toString().equals(token.getStatus()) ||
                                TokenStatus.IN_SERVICE.toString().equals(token.getStatus())));

        if (hasActiveToken) {
            throw new UserAlreadyInQueueException("User already has an active token in this queue");
        }

        // Check if queue has reached max capacity
        long waitingAndInServiceTokens = queue.getTokens().stream()
                .filter(token -> TokenStatus.WAITING.toString().equals(token.getStatus()) ||
                        TokenStatus.IN_SERVICE.toString().equals(token.getStatus()))
                .count();

        if (queue.getMaxCapacity() != null && waitingAndInServiceTokens >= queue.getMaxCapacity()) {
            throw new IllegalStateException("Queue has reached its maximum capacity. Please try again later.");
        }

        int nextToken = queue.getTokenCounter() + 1;
        queue.setTokenCounter(nextToken);
        String tokenId = "T-" + String.format("%03d", nextToken);

        QueueToken token = new QueueToken(tokenId, userId, TokenStatus.WAITING.toString(), LocalDateTime.now());
        queue.getTokens().add(token);

        Queue updatedQueue = queueRepository.save(queue);
        broadcastQueueUpdate(queueId, updatedQueue);

        log.debug("Token {} added to queueId={}", tokenId, queueId);
        return token;
    }

    /** 👥👥 User joins queue with group */
    public QueueToken addGroupToken(String queueId, String userId, List<QueueToken.GroupMember> groupMembers) {
        Queue queue = getQueueOrThrow(queueId);

        if (!queue.getIsActive()) {
            logger.warn("Inactive queue join attempt: queueId={}", queueId);
            throw new QueueInactiveException("Provider is on break. Queue temporarily unavailable.");
        }

        if (!queue.getSupportsGroupToken()) {
            throw new UnsupportedOperationException("This queue does not support group tokens");
        }

        // Check if user already has an active token in this queue
        boolean hasActiveToken = queue.getTokens().stream()
                .anyMatch(token -> token.getUserId().equals(userId) &&
                        (TokenStatus.WAITING.toString().equals(token.getStatus()) ||
                                TokenStatus.IN_SERVICE.toString().equals(token.getStatus())));

        if (hasActiveToken) {
            throw new UserAlreadyInQueueException("User already has an active token in this queue");
        }

        long waitingAndInServiceTokens = queue.getTokens().stream()
                .filter(token -> TokenStatus.WAITING.toString().equals(token.getStatus()) ||
                        TokenStatus.IN_SERVICE.toString().equals(token.getStatus()))
                .count();

        if (queue.getMaxCapacity() != null && waitingAndInServiceTokens >= queue.getMaxCapacity()) {
            throw new IllegalStateException("Queue has reached its maximum capacity. Please try again later.");
        }

        if (groupMembers == null || groupMembers.size() < 2) {
            throw new IllegalArgumentException("Group must have at least 2 members");
        }

        int nextToken = queue.getTokenCounter() + 1;
        queue.setTokenCounter(nextToken);
        String tokenId = "G-" + String.format("%03d", nextToken);

        QueueToken token = new QueueToken(tokenId, userId, TokenStatus.WAITING.toString(),
                LocalDateTime.now(), groupMembers, groupMembers.size());
        queue.getTokens().add(token);

        Queue updatedQueue = queueRepository.save(queue);
        broadcastQueueUpdate(queueId, updatedQueue);

        logger.debug("Group token {} added to queueId={} with {} members", tokenId, queueId, groupMembers.size());
        return token;
    }

    /** 🚑 User joins queue with emergency */
    public QueueToken addEmergencyToken(String queueId, String userId, String emergencyDetails) {
        Queue queue = getQueueOrThrow(queueId);

        if (!queue.getIsActive()) {
            logger.warn("Inactive queue join attempt: queueId={}", queueId);
            throw new QueueInactiveException("Provider is on break. Queue temporarily unavailable.");
        }

        if (!queue.getEmergencySupport()) {
            throw new UnsupportedOperationException("This queue does not support emergency tokens");
        }

        // Check if user already has an active token in this queue
        boolean hasActiveToken = queue.getTokens().stream()
                .anyMatch(token -> token.getUserId().equals(userId) &&
                        (TokenStatus.WAITING.toString().equals(token.getStatus()) ||
                                TokenStatus.IN_SERVICE.toString().equals(token.getStatus())));

        if (hasActiveToken) {
            throw new UserAlreadyInQueueException("User already has an active token in this queue");
        }

        long waitingAndInServiceTokens = queue.getTokens().stream()
                .filter(token -> TokenStatus.WAITING.toString().equals(token.getStatus()) ||
                        TokenStatus.IN_SERVICE.toString().equals(token.getStatus()))
                .count();

        if (queue.getMaxCapacity() != null && waitingAndInServiceTokens >= queue.getMaxCapacity()) {
            throw new IllegalStateException("Queue has reached its maximum capacity. Please try again later.");
        }


        int nextToken = queue.getTokenCounter() + 1;
        queue.setTokenCounter(nextToken);
        String tokenId = "E-" + String.format("%03d", nextToken);

        QueueToken token = new QueueToken(tokenId, userId, TokenStatus.WAITING.toString(),
                LocalDateTime.now(), emergencyDetails, queue.getEmergencyPriorityWeight());
        queue.getTokens().add(token);

        Queue updatedQueue = queueRepository.save(queue);
        broadcastQueueUpdate(queueId, updatedQueue);

        logger.debug("Emergency token {} added to queueId={}", tokenId, queueId);
        return token;
    }

    /** 📡 Broadcast helper */
    private void broadcastQueueUpdate(String queueId, Queue queue) {
        // Send to the specific queue topic
        messagingTemplate.convertAndSend("/topic/queues/" + queueId, queue);

        // Also send to a general topic for all queue updates
        messagingTemplate.convertAndSend("/topic/queues", queue);

        // Send to the place's queue topic for UI updates
        messagingTemplate.convertAndSend("/topic/places/" + queue.getPlaceId() + "/queues", queue);
    }


    /** ⏩ Serve next waiting token */
    public Queue serveNextToken(String queueId) {
        Queue queue = getQueueOrThrow(queueId);

        // First, complete any currently in-service token
        queue.getTokens().stream()
                .filter(t -> TokenStatus.IN_SERVICE.toString().equals(t.getStatus()))
                .findFirst()
                .ifPresent(inServiceToken -> {
                    inServiceToken.setStatus(TokenStatus.COMPLETED.toString());
                    inServiceToken.setCompletedAt(LocalDateTime.now());
                    log.info("Completed previous in-service token: {}", inServiceToken.getTokenId());
                });

        // Then find the next WAITING token (considering priority)
        Optional<QueueToken> nextToken = queue.getTokens().stream()
                .filter(t -> TokenStatus.WAITING.toString().equals(t.getStatus()))
                .max((t1, t2) -> Integer.compare(t1.getPriority(), t2.getPriority()));

        if (nextToken.isPresent()) {
            QueueToken token = nextToken.get();
            token.setStatus(TokenStatus.IN_SERVICE.toString());
            token.setServedAt(LocalDateTime.now());

            Queue updatedQueue = queueRepository.save(queue);
            broadcastQueueUpdate(queueId, updatedQueue);
            log.info("Token {} moved to IN_SERVICE", token.getTokenId());
            return updatedQueue;
        }

        log.info("No waiting tokens in queueId={}", queueId);
        return queue;
    }

    /** 🔁 Toggle queue active status */
    public Queue setQueueActiveStatus(String queueId, boolean active) {
        Queue queue = getQueueOrThrow(queueId);
        queue.setIsActive(active);
        Queue updatedQueue = queueRepository.save(queue);
        broadcastQueueUpdate(queueId, updatedQueue);
        log.info("Queue {} active status changed to {}", queueId, active);
        return updatedQueue;
    }

    /** 🕐 Scheduled method to update estimated wait times */
    @Scheduled(fixedRate = 30000) // Every 30 seconds
    public void updateAllQueueWaitTimes() {
        log.info("🕐 Updating estimated wait times for all queues");
        List<Queue> allQueues = queueRepository.findAll();

        for (Queue queue : allQueues) {
            try {
                int waitingTokens = (int) queue.getTokens().stream()
                        .filter(t -> TokenStatus.WAITING.toString().equals(t.getStatus()))
                        .count();

                // Simple calculation: 5 minutes per token
                int estimatedWaitTime = waitingTokens * 5;
                queue.setEstimatedWaitTime(estimatedWaitTime);

                queueRepository.save(queue);

                // Broadcast update if queue is active
                if (queue.getIsActive()) {
                    broadcastQueueUpdate(queue.getId(), queue);
                }
            } catch (Exception e) {
                log.error("Error updating wait time for queue {}: {}", queue.getId(), e.getMessage());
            }
        }
    }


    /** 🆕 Create a new queue with place and service references */
    public Queue createNewQueue(String providerId, String serviceName, String placeId, String serviceId) {
        logger.info("Creating queue for providerId={}, serviceName={}, placeId={}, serviceId={}",
                providerId, serviceName, placeId, serviceId);

        Queue newQueue = new Queue(providerId, serviceName, placeId, serviceId);
        return queueRepository.save(newQueue);
    }

    /** 🆕 Create a new queue with advanced settings */
    public Queue createNewQueue(String providerId, String serviceName, String placeId, String serviceId,
                                Integer maxCapacity, Boolean supportsGroupToken, Boolean emergencySupport, Integer emergencyPriorityWeight) {
        logger.info("Creating queue with advanced settings for providerId={}", providerId);

        Queue newQueue = new Queue(providerId, serviceName, placeId, serviceId);
        newQueue.setMaxCapacity(maxCapacity);
        newQueue.setSupportsGroupToken(supportsGroupToken != null ? supportsGroupToken : false);
        newQueue.setEmergencySupport(emergencySupport != null ? emergencySupport : false);
        newQueue.setEmergencyPriorityWeight(emergencyPriorityWeight != null ? emergencyPriorityWeight : 10);

        return queueRepository.save(newQueue);
    }

    public List<Queue> getQueuesByProviderId(String providerId) {
        logger.info("Fetching queues for providerId={}", providerId);
        List<Queue> queues = queueRepository.findByProviderId(providerId);

        // Ensure the provider only accesses their own queues
        queues = queues.stream()
                .filter(queue -> queue.getProviderId().equals(providerId))
                .collect(Collectors.toList());

        logger.debug("Filtered to {} queues for providerId={}", queues.size(), providerId);
        return queues;
    }

    /** 📋 Get all queues for a place */
    public List<Queue> getQueuesByPlaceId(String placeId) {
        logger.info("Fetching queues for placeId={}", placeId);
        return queueRepository.findByPlaceId(placeId);
    }

    /** 📋 Get all queues for a service */
    public List<Queue> getQueuesByServiceId(String serviceId) {
        logger.info("Fetching queues for serviceId={}", serviceId);
        return queueRepository.findByServiceId(serviceId);
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

    /** 🆕 Update queue statistics */
    public Queue updateQueueStatistics(String queueId) {
        Queue queue = getQueueOrThrow(queueId);

        // Calculate average wait time
        // This is a simplified implementation
        // In a real scenario, you would track actual wait times
        if (queue.getStatistics() == null) {
            queue.setStatistics(new Queue.QueueStatistics());
        }

        // Update statistics based on queue data
        // This would be more complex in a real implementation
        queue.getStatistics().setTotalServed(
                (int) queue.getTokens().stream()
                        .filter(t -> TokenStatus.COMPLETED.toString().equals(t.getStatus()))
                        .count()
        );

        Queue updatedQueue = queueRepository.save(queue);
        broadcastQueueUpdate(queueId, updatedQueue);

        logger.info("Updated statistics for queue {}", queueId);
        return updatedQueue;
    }

    // In QueueService.java
    @Scheduled(fixedRate = 60000) // Run every minute
    public void cleanupExpiredTokens() {
        logger.info("Cleaning up expired tokens...");
        List<Queue> allQueues = queueRepository.findAll();

        for (Queue queue : allQueues) {
            boolean modified = false;
            Iterator<QueueToken> iterator = queue.getTokens().iterator();

            while (iterator.hasNext()) {
                QueueToken token = iterator.next();
                // Remove tokens older than 24 hours
                if (token.getIssuedAt().isBefore(LocalDateTime.now().minusHours(24))) {
                    iterator.remove();
                    modified = true;
                    logger.info("Removed expired token: {}", token.getTokenId());
                }
            }

            if (modified) {
                queueRepository.save(queue);
                broadcastQueueUpdate(queue.getId(), queue);
            }
        }
    }

    public List<Queue> getQueuesByUserId(String userId) {
        // Find all queues that contain at least one token for the given userId
        return queueRepository.findAll().stream()
                .filter(queue -> queue.getTokens().stream()
                        .anyMatch(token -> token.getUserId().equals(userId)))
                .collect(Collectors.toList());
    }
}