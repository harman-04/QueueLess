package com.queueless.backend.service;

import com.queueless.backend.enums.TokenStatus;
import com.queueless.backend.exception.QueueInactiveException;
import com.queueless.backend.exception.ResourceNotFoundException;
import com.queueless.backend.exception.UserAlreadyInQueueException;
import com.queueless.backend.model.*;
import com.queueless.backend.repository.FeedbackRepository;
import com.queueless.backend.repository.QueueRepository;
import com.queueless.backend.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;


import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class QueueService {

    private static final Logger logger = LoggerFactory.getLogger(QueueService.class);

    private final QueueRepository queueRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;
    private final PlaceService placeService;
    private final ServiceService serviceService;
    private final FeedbackService feedbackService;
    private final FeedbackRepository feedbackRepository;

    private Queue getQueueOrThrow(String queueId) {
        return queueRepository.findById(queueId)
                .orElseThrow(() -> {
                    log.error("Queue not found with id {}", queueId);
                    return new ResourceNotFoundException("Queue not found with id " + queueId);
                });
    }

    public QueueToken addNewToken(String queueId, String userId) {
        Queue queue = getQueueOrThrow(queueId);

        if (!queue.getIsActive()) {
            log.warn("Inactive queue join attempt: queueId={}", queueId);
            throw new QueueInactiveException("Provider is on break. Queue temporarily unavailable.");
        }

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
        String tokenId = "T-" + String.format("%03d", nextToken);

        QueueToken token = new QueueToken(tokenId, userId, TokenStatus.WAITING.toString(), LocalDateTime.now());
        queue.getTokens().add(token);

        Queue updatedQueue = queueRepository.save(queue);
        broadcastQueueUpdate(queueId, updatedQueue);

        log.debug("Token {} added to queueId={}", tokenId, queueId);
        return token;
    }

    public QueueToken addGroupToken(String queueId, String userId, List<QueueToken.GroupMember> groupMembers) {
        Queue queue = getQueueOrThrow(queueId);

        if (!queue.getIsActive()) {
            logger.warn("Inactive queue join attempt: queueId={}", queueId);
            throw new QueueInactiveException("Provider is on break. Queue temporarily unavailable.");
        }

        if (!queue.getSupportsGroupToken()) {
            throw new UnsupportedOperationException("This queue does not support group tokens");
        }

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

    public QueueToken addEmergencyToken(String queueId, String userId, String emergencyDetails) {
        Queue queue = getQueueOrThrow(queueId);

        if (!queue.getIsActive()) {
            logger.warn("Inactive queue join attempt: queueId={}", queueId);
            throw new QueueInactiveException("Provider is on break. Queue temporarily unavailable.");
        }

        if (!queue.getEmergencySupport()) {
            throw new UnsupportedOperationException("This queue does not support emergency tokens");
        }

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

    private void broadcastQueueUpdate(String queueId, Queue queue) {
        messagingTemplate.convertAndSend("/topic/queues/" + queueId, queue);
        messagingTemplate.convertAndSend("/topic/queues", queue);
        messagingTemplate.convertAndSend("/topic/places/" + queue.getPlaceId() + "/queues", queue);
    }

    public Queue serveNextToken(String queueId) {
        Queue queue = getQueueOrThrow(queueId);

        queue.getTokens().stream()
                .filter(t -> TokenStatus.IN_SERVICE.toString().equals(t.getStatus()))
                .findFirst()
                .ifPresent(inServiceToken -> {
                    inServiceToken.setStatus(TokenStatus.COMPLETED.toString());
                    inServiceToken.setCompletedAt(LocalDateTime.now());
                    log.info("Completed previous in-service token: {}", inServiceToken.getTokenId());
                });

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

    // Update the createFeedbackOpportunity method
    private void createFeedbackOpportunity(QueueToken token, Queue queue) {
        try {
            // Check if feedback already exists
            Optional<Feedback> existingFeedback = feedbackRepository.findByTokenId(token.getTokenId());
            if (existingFeedback.isPresent()) {
                return; // Feedback already exists
            }

            Feedback feedback = new Feedback(
                    queue.getId(),
                    token.getTokenId(),
                    token.getUserId(),
                    queue.getProviderId(),
                    queue.getPlaceId(),
                    queue.getServiceId()
            );

            feedbackRepository.save(feedback);
            logger.info("Feedback opportunity created for token: {}", token.getTokenId());

        } catch (Exception e) {
            logger.error("Error creating feedback opportunity: {}", e.getMessage());
        }
    }

    public Queue setQueueActiveStatus(String queueId, boolean active) {
        Queue queue = getQueueOrThrow(queueId);
        queue.setIsActive(active);
        Queue updatedQueue = queueRepository.save(queue);
        broadcastQueueUpdate(queueId, updatedQueue);
        log.info("Queue {} active status changed to {}", queueId, active);
        return updatedQueue;
    }

    @Scheduled(fixedRate = 30000)
    public void updateAllQueueWaitTimes() {
        log.info("🕐 Updating estimated wait times for all queues");
        List<Queue> allQueues = queueRepository.findAll();

        for (Queue queue : allQueues) {
            try {
                int waitingTokens = (int) queue.getTokens().stream()
                        .filter(t -> TokenStatus.WAITING.toString().equals(t.getStatus()))
                        .count();

                int estimatedWaitTime = waitingTokens * 5;
                queue.setEstimatedWaitTime(estimatedWaitTime);

                queueRepository.save(queue);

                if (queue.getIsActive()) {
                    broadcastQueueUpdate(queue.getId(), queue);
                }
            } catch (Exception e) {
                log.error("Error updating wait time for queue {}: {}", queue.getId(), e.getMessage());
            }
        }
    }

    public Queue createNewQueue(String providerId, String serviceName, String placeId, String serviceId) {
        logger.info("Creating queue for providerId={}, serviceName={}, placeId={}, serviceId={}",
                providerId, serviceName, placeId, serviceId);

        Queue newQueue = new Queue(providerId, serviceName, placeId, serviceId);
        return queueRepository.save(newQueue);
    }

    public Queue createNewQueue(String providerId, String serviceName, String placeId, String serviceId,
                                Integer maxCapacity, Boolean supportsGroupToken, Boolean emergencySupport, Integer emergencyPriorityWeight) {
        logger.info("Creating queue with advanced settings for providerId={}", providerId);

        User provider = userRepository.findById(providerId)
                .orElseThrow(() -> new RuntimeException("Provider not found"));

        Place place = placeService.getPlaceById(placeId);
        if (place == null) {
            throw new RuntimeException("Place not found");
        }

        if (!place.getAdminId().equals(provider.getAdminId()) &&
                (provider.getManagedPlaceIds() == null || !provider.getManagedPlaceIds().contains(placeId))) {
            throw new RuntimeException("Provider does not have access to this place");
        }

        Service service = serviceService.getServiceById(serviceId);
        if (service == null) {
            throw new RuntimeException("Service not found");
        }

        if (!service.getPlaceId().equals(placeId)) {
            throw new RuntimeException("Service does not belong to the selected place");
        }

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
        queues = queues.stream()
                .filter(queue -> queue.getProviderId().equals(providerId))
                .collect(Collectors.toList());
        logger.debug("Filtered to {} queues for providerId={}", queues.size(), providerId);
        return queues;
    }

    public List<Queue> getQueuesByPlaceId(String placeId) {
        logger.info("Fetching queues for placeId={}", placeId);
        return queueRepository.findByPlaceId(placeId);
    }

    public List<Queue> getQueuesByServiceId(String serviceId) {
        logger.info("Fetching queues for serviceId={}", serviceId);
        return queueRepository.findByServiceId(serviceId);
    }

    public Queue getQueueById(String queueId) {
        return getQueueOrThrow(queueId);
    }

    public List<Queue> getAllQueues() {
        logger.info("Fetching all active queues");
        return queueRepository.findByIsActive(true);
    }

    // In QueueService.java - Update the completeToken method
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
        token.setCompletedAt(LocalDateTime.now());
        Queue updatedQueue = queueRepository.save(queue);
        broadcastQueueUpdate(queueId, updatedQueue);

        // Create feedback opportunity
        logger.info("Token {} marked COMPLETED", tokenId);
        return updatedQueue;
    }

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

    public Queue reorderQueue(String queueId, List<QueueToken> newTokens) {
        Queue queue = getQueueOrThrow(queueId);
        queue.setTokens(newTokens);
        Queue updatedQueue = queueRepository.save(queue);
        broadcastQueueUpdate(queueId, updatedQueue);

        logger.info("Queue reordered for queueId={}", queueId);
        return updatedQueue;
    }

    @PostConstruct
    public void cleanupInconsistentTokenStatuses() {
        logger.info("Cleaning up inconsistent token statuses...");
        List<Queue> allQueues = queueRepository.findAll();

        for (Queue queue : allQueues) {
            boolean needsUpdate = false;

            long inServiceCount = queue.getTokens().stream()
                    .filter(t -> TokenStatus.IN_SERVICE.toString().equals(t.getStatus()))
                    .count();

            if (inServiceCount > 1) {
                logger.warn("Queue {} has {} IN_SERVICE tokens, fixing...", queue.getId(), inServiceCount);

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

    public Queue updateQueueStatistics(String queueId) {
        Queue queue = getQueueOrThrow(queueId);

        if (queue.getStatistics() == null) {
            queue.setStatistics(new Queue.QueueStatistics());
        }

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

    @Scheduled(fixedRate = 60000)
    public void cleanupExpiredTokens() {
        logger.info("Cleaning up expired tokens...");
        List<Queue> allQueues = queueRepository.findAll();

        for (Queue queue : allQueues) {
            boolean modified = false;
            Iterator<QueueToken> iterator = queue.getTokens().iterator();

            while (iterator.hasNext()) {
                QueueToken token = iterator.next();
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
        return queueRepository.findAll().stream()
                .filter(queue -> queue.getTokens().stream()
                        .anyMatch(token -> token.getUserId().equals(userId)))
                .collect(Collectors.toList());
    }


}