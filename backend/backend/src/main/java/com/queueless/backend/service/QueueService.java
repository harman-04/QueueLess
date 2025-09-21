package com.queueless.backend.service;

import com.queueless.backend.dto.QueueResetRequestDTO;
import com.queueless.backend.dto.QueueResetResponseDTO;
import com.queueless.backend.dto.TokenRequestDTO;
import com.queueless.backend.dto.UserDetailsResponseDTO;
import com.queueless.backend.enums.Role;
import com.queueless.backend.enums.TokenStatus;
import com.queueless.backend.exception.AccessDeniedException;
import com.queueless.backend.exception.QueueInactiveException;
import com.queueless.backend.exception.ResourceNotFoundException;
import com.queueless.backend.exception.UserAlreadyInQueueException;
import com.queueless.backend.model.*;
import com.queueless.backend.model.Queue;
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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class QueueService {


    private final ConcurrentHashMap<String, LocalDateTime> userQueueJoins = new ConcurrentHashMap<>();
    private static final long JOIN_COOLDOWN_MINUTES = 30;

    private static final Logger logger = LoggerFactory.getLogger(QueueService.class);

    private final QueueRepository queueRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;
    private final PlaceService placeService;
    private final ServiceService serviceService;
    private final FeedbackService feedbackService;
    private final FeedbackRepository feedbackRepository;
    private final ExportService exportService;
    private final ExportCacheService exportCacheService;

    // Add these missing methods
    public LocalDateTime getLastQueueJoinTime(String userId) {
        return userQueueJoins.get(userId);
    }

    public boolean canUserJoinQueue(String userId) {
        LocalDateTime lastJoin = userQueueJoins.get(userId);
        if (lastJoin == null) return true;

        return lastJoin.plusMinutes(JOIN_COOLDOWN_MINUTES).isBefore(LocalDateTime.now());
    }

    public long getJoinCooldownMinutes() {
        return JOIN_COOLDOWN_MINUTES;
    }

    private boolean hasActiveQueueParticipation(String userId) {
        return !canUserJoinQueue(userId);
    }

    private Queue getQueueOrThrow(String queueId) {
        return queueRepository.findById(queueId)
                .orElseThrow(() -> {
                    log.error("Queue not found with id {}", queueId);
                    return new ResourceNotFoundException("Queue not found with id " + queueId);
                });
    }

    public QueueToken addNewTokenWithDetails(String queueId, String userId, TokenRequestDTO tokenRequest) {
        Queue queue = getQueueOrThrow(queueId);

        if (!queue.getIsActive()) {
            log.warn("Inactive queue join attempt: queueId={}", queueId);
            throw new QueueInactiveException("Provider is on break. Queue temporarily unavailable.");
        }

        if (hasActiveQueueParticipation(userId)) {
            throw new UserAlreadyInQueueException("You can only join one queue at a time. Please complete or cancel your current queue participation.");
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

        // Get user details for storing with token
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        int nextToken = queue.getTokenCounter() + 1;
        queue.setTokenCounter(nextToken);
        String tokenId = "T-" + String.format("%03d", nextToken);

        // Create user details object
        UserQueueDetails userDetails = new UserQueueDetails();
        userDetails.setPurpose(tokenRequest.getPurpose());
        userDetails.setCondition(tokenRequest.getCondition());
        userDetails.setNotes(tokenRequest.getNotes());
        userDetails.setCustomFields(tokenRequest.getCustomFields());
        userDetails.setIsPrivate(tokenRequest.getIsPrivate());
        userDetails.setVisibleToProvider(tokenRequest.getVisibleToProvider());
        userDetails.setVisibleToAdmin(tokenRequest.getVisibleToAdmin());

        QueueToken token = new QueueToken(tokenId, userId, user.getName(), TokenStatus.WAITING.toString(), LocalDateTime.now());
        token.setUserDetails(userDetails);

        queue.getTokens().add(token);
        userQueueJoins.put(userId, LocalDateTime.now());

        Queue updatedQueue = queueRepository.save(queue);
        broadcastQueueUpdate(queueId, updatedQueue);

        log.debug("Token {} with user details added to queueId={}", tokenId, queueId);
        return token;
    }

    public UserDetailsResponseDTO getUserDetailsForToken(String queueId, String tokenId, String requesterId, Role requesterRole) {
        Queue queue = getQueueOrThrow(queueId);

        QueueToken token = queue.getTokens().stream()
                .filter(t -> t.getTokenId().equals(tokenId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Token not found"));

        // Check if requester has permission to view details
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean canViewDetails = false;

        if (requesterRole == Role.ADMIN) {
            canViewDetails = token.getUserDetails() == null ||
                    token.getUserDetails().getVisibleToAdmin() == null ||
                    token.getUserDetails().getVisibleToAdmin();
        } else if (requesterRole == Role.PROVIDER && queue.getProviderId().equals(requesterId)) {
            canViewDetails = token.getUserDetails() == null ||
                    token.getUserDetails().getVisibleToProvider() == null ||
                    token.getUserDetails().getVisibleToProvider();
        } else if (requesterId.equals(token.getUserId())) {
            // Users can always view their own details
            canViewDetails = true;
        }

        if (!canViewDetails) {
            throw new AccessDeniedException("You don't have permission to view these details");
        }

        UserDetailsResponseDTO response = new UserDetailsResponseDTO();
        response.setUserId(token.getUserId());
        response.setUserName(token.getUserName());
        response.setDetailsVisible(canViewDetails);

        if (token.getUserDetails() != null && canViewDetails) {
            // Apply privacy settings
            boolean isPrivate = token.getUserDetails().getIsPrivate() != null &&
                    token.getUserDetails().getIsPrivate();

            if (!isPrivate) {
                response.setPurpose(token.getUserDetails().getPurpose());
                response.setCondition(token.getUserDetails().getCondition());
                response.setNotes(token.getUserDetails().getNotes());
                response.setCustomFields(token.getUserDetails().getCustomFields());
            }
        }

        return response;
    }

    public QueueResetResponseDTO resetQueueWithOptions(String queueId, QueueResetRequestDTO resetRequest, String requesterId) {
        Queue queue = getQueueOrThrow(queueId);

        // Verify requester has permission to reset this queue
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean canReset = requester.getRole() == Role.ADMIN ||
                (requester.getRole() == Role.PROVIDER && queue.getProviderId().equals(requesterId));

        if (!canReset) {
            throw new AccessDeniedException("You don't have permission to reset this queue");
        }

        QueueResetResponseDTO response = new QueueResetResponseDTO();

        // Preserve data if requested
        if (resetRequest.getPreserveData() != null && resetRequest.getPreserveData()) {
            try {
                // Export current queue data with user details if requested
                byte[] exportData = exportService.exportQueueToPdf(queue,
                        resetRequest.getReportType() != null ? resetRequest.getReportType() : "full",
                        resetRequest.getIncludeUserDetails());



                // In a real implementation, you would save this to cloud storage or a file system
                // For now, we'll just log it and generate a mock URL
                String exportId = "export-" + System.currentTimeMillis() + "-" + queueId;
                exportCacheService.saveExport(exportId, exportData);
                log.info("Queue data exported for queue {} with ID {}", queueId, exportId);

                response.setExportFileUrl("/export/exports/" + exportId);
            } catch (Exception e) {
                log.error("Failed to export queue data before reset: {}", e.getMessage());
                throw new RuntimeException("Failed to export queue data: " + e.getMessage());
            }
        }

        // Count tokens being reset
        int tokensReset = queue.getTokens().size();

        // Collect user IDs of all tokens that will be removed
        Set<String> affectedUserIds = queue.getTokens().stream()
                .map(QueueToken::getUserId)
                .collect(Collectors.toSet());


        // Reset the queue
        queue.getTokens().clear();
        queue.setTokenCounter(0);
        queue.setCurrentPosition(0);
        queue.setStartTime(LocalDateTime.now());

        // Reset statistics if needed
        if (queue.getStatistics() != null) {
            queue.getStatistics().setDailyUsersServed(0);
        }

        queueRepository.save(queue);
        broadcastQueueUpdate(queueId, queue);
        // Remove user tracking for the affected users
        affectedUserIds.forEach(userId -> {
            userQueueJoins.remove(userId);
            log.info("Removed user {} from tracking due to queue reset", userId);
        });

        response.setSuccess(true);
        response.setMessage("Queue reset successfully");
        response.setTokensReset(tokensReset);

        log.info("Queue {} reset by user {}, {} tokens cleared", queueId, requesterId, tokensReset);
        return response;
    }
    // Existing methods with user restriction checks
    public QueueToken addNewToken(String queueId, String userId) {
        Queue queue = getQueueOrThrow(queueId);

        if (!queue.getIsActive()) {
            log.warn("Inactive queue join attempt: queueId={}", queueId);
            throw new QueueInactiveException("Provider is on break. Queue temporarily unavailable.");
        }

        if (hasActiveQueueParticipation(userId)) {
            throw new UserAlreadyInQueueException("You can only join one queue at a time. Please complete or cancel your current queue participation.");
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

        userQueueJoins.put(userId, LocalDateTime.now());

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

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        QueueToken token = new QueueToken(tokenId, userId, user.getName(), TokenStatus.WAITING.toString(),
                LocalDateTime.now(), groupMembers, groupMembers.size());

        queue.getTokens().add(token);

        Queue updatedQueue = queueRepository.save(queue);
        broadcastQueueUpdate(queueId, updatedQueue);

        logger.debug("Group token {} added to queueId={} with {} members", tokenId, queueId, groupMembers.size());
        return token;
    }

    // Update addEmergencyToken method
    public QueueToken addEmergencyToken(String queueId, String userId, String emergencyDetails) {
        Queue queue = getQueueOrThrow(queueId);

        if (!queue.getIsActive()) {
            log.warn("Inactive queue join attempt: queueId={}", queueId);
            throw new QueueInactiveException("Provider is on break. Queue temporarily unavailable.");
        }

        if (!queue.getEmergencySupport()) {
            throw new UnsupportedOperationException("This queue does not support emergency tokens");
        }

        if (hasActiveQueueParticipation(userId)) {
            throw new UserAlreadyInQueueException("You can only join one queue at a time");
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

        // Get user name
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        QueueToken token;
        if (queue.getAutoApproveEmergency()) {
            // Auto-approve emergency token
            token = new QueueToken(tokenId, userId, user.getName(), TokenStatus.WAITING.toString(),
                    LocalDateTime.now(), emergencyDetails, queue.getEmergencyPriorityWeight());
            queue.getTokens().add(token);
            userQueueJoins.put(userId, LocalDateTime.now());
        } else {
            // Require provider approval
            token = new QueueToken(tokenId, userId, user.getName(), TokenStatus.PENDING.toString(),
                    LocalDateTime.now(), emergencyDetails, queue.getEmergencyPriorityWeight());
            queue.getPendingEmergencyTokens().add(token);
        }
        Queue updatedQueue = queueRepository.save(queue);
        broadcastQueueUpdate(queueId, updatedQueue);

        log.debug("Emergency token {} added to queueId={}", tokenId, queueId);
        return token;
    }

    // Emergency approval methods
    public Queue approveEmergencyToken(String queueId, String tokenId, boolean approve, String reason) {
        Queue queue = getQueueOrThrow(queueId);

        Optional<QueueToken> pendingToken = queue.getPendingEmergencyTokens().stream()
                .filter(t -> t.getTokenId().equals(tokenId))
                .findFirst();

        if (pendingToken.isEmpty()) {
            throw new ResourceNotFoundException("Pending emergency token not found");
        }

        QueueToken token = pendingToken.get();

        if (approve) {
            // Move to active tokens
            token.setStatus(TokenStatus.WAITING.toString());
            queue.getTokens().add(token);
            userQueueJoins.put(token.getUserId(), LocalDateTime.now());

            // Notify user
            messagingTemplate.convertAndSendToUser(
                    token.getUserId(),
                    "/queue/emergency-approved",
                    Map.of(
                            "tokenId", tokenId,
                            "queueId", queueId,
                            "approved", true,
                            "message", "Your emergency token has been approved"
                    )
            );
        } else {
            // Notify user of rejection
            messagingTemplate.convertAndSendToUser(
                    token.getUserId(),
                    "/queue/emergency-approved",
                    Map.of(
                            "tokenId", tokenId,
                            "queueId", queueId,
                            "approved", false,
                            "message", reason != null ? reason : "Your emergency request was rejected"
                    )
            );
        }

        // Remove from pending
        queue.getPendingEmergencyTokens().removeIf(t -> t.getTokenId().equals(tokenId));

        Queue updatedQueue = queueRepository.save(queue);
        broadcastQueueUpdate(queueId, updatedQueue);

        return updatedQueue;
    }

    public List<QueueToken> getPendingEmergencyTokens(String queueId) {
        Queue queue = getQueueOrThrow(queueId);
        return queue.getPendingEmergencyTokens();
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

    private void createFeedbackOpportunity(QueueToken token, Queue queue) {
        try {
            Optional<Feedback> existingFeedback = feedbackRepository.findByTokenId(token.getTokenId());
            if (existingFeedback.isPresent()) {
                return;
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
            log.info("Feedback opportunity created for token: {}", token.getTokenId());
        } catch (Exception e) {
            log.error("Error creating feedback opportunity: {}", e.getMessage());
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

    // Enhanced createNewQueue method
    public Queue createNewQueue(String providerId, String serviceName, String placeId, String serviceId,
                                Integer maxCapacity, Boolean supportsGroupToken, Boolean emergencySupport,
                                Integer emergencyPriorityWeight, Boolean requiresEmergencyApproval,
                                Boolean autoApproveEmergency) {
        log.info("Creating queue with advanced settings for providerId={}", providerId);

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

        // Set emergency approval settings
        newQueue.setRequiresEmergencyApproval(requiresEmergencyApproval != null ? requiresEmergencyApproval : false);
        newQueue.setAutoApproveEmergency(autoApproveEmergency != null ? autoApproveEmergency : false);

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

    // Update completion to remove from tracking
    public Queue completeToken(String queueId, String tokenId) {
        Queue queue = getQueueOrThrow(queueId);

        QueueToken token = queue.getTokens().stream()
                .filter(t -> t.getTokenId().equals(tokenId))
                .findFirst()
                .orElseThrow(() -> {
                    log.error("Token not found: {}", tokenId);
                    return new ResourceNotFoundException("Token not found with id " + tokenId);
                });

        token.setStatus(TokenStatus.COMPLETED.toString());
        token.setCompletedAt(LocalDateTime.now());

        // Add duration calculation
        if (token.getServedAt() != null) {
            long durationInMinutes = java.time.Duration.between(token.getServedAt(), token.getCompletedAt()).toMinutes();
            token.setServiceDurationMinutes(durationInMinutes); // You'll need to add this field to your QueueToken model
        }

        // Remove from user tracking
        userQueueJoins.remove(token.getUserId());

        Queue updatedQueue = queueRepository.save(queue);
        broadcastQueueUpdate(queueId, updatedQueue);

        log.info("Token {} marked COMPLETED", tokenId);
        return updatedQueue;
    }

    // Scheduled cleanup for tracking map
    @Scheduled(fixedRate = 3600000) // Cleanup every hour
    public void cleanupUserQueueTracking() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(JOIN_COOLDOWN_MINUTES);
        userQueueJoins.entrySet().removeIf(entry ->
                entry.getValue().isBefore(cutoffTime)
        );
        log.info("Cleaned up user queue tracking. Current size: {}", userQueueJoins.size());
    }

    // Update cancellation to remove from tracking
    public Queue cancelToken(String queueId, String tokenId) {
        Queue queue = getQueueOrThrow(queueId);

        Optional<QueueToken> tokenToCancel = queue.getTokens().stream()
                .filter(t -> t.getTokenId().equals(tokenId))
                .findFirst();

        boolean removed = queue.getTokens().removeIf(t -> t.getTokenId().equals(tokenId));
        if (!removed) {
            // Check if it's a pending emergency token
            removed = queue.getPendingEmergencyTokens().removeIf(t -> t.getTokenId().equals(tokenId));

            if (!removed) {
                log.error("Token not found for cancellation: {}", tokenId);
                throw new ResourceNotFoundException("Token not found with id " + tokenId);
            }
        }

        // Remove from user tracking if it was an active token
        if (tokenToCancel.isPresent()) {
            userQueueJoins.remove(tokenToCancel.get().getUserId());
        }

        Queue updatedQueue = queueRepository.save(queue);
        broadcastQueueUpdate(queueId, updatedQueue);

        log.info("Token {} cancelled", tokenId);
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

    public Integer calculateCurrentWaitTime(String queueId) {
        Queue queue = getQueueOrThrow(queueId);

        if (queue.getTokens() == null || queue.getTokens().isEmpty()) {
            return 0;
        }

        long waitingTokens = queue.getTokens().stream()
                .filter(t -> TokenStatus.WAITING.toString().equals(t.getStatus()))
                .count();

        Service service = serviceService.getServiceById(queue.getServiceId());
        Integer averageServiceTime = service != null ? service.getAverageServiceTime() : 5;

        return (int) (waitingTokens * averageServiceTime);
    }
}