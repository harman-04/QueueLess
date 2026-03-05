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
import com.queueless.backend.repository.QueueHourlyStatsRepository;
import com.queueless.backend.repository.QueueRepository;
import com.queueless.backend.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class QueueService {

    private final QueueRepository queueRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;
    private final PlaceService placeService;
    private final ServiceService serviceService;
    private final FeedbackService feedbackService;
    private final FeedbackRepository feedbackRepository;
    private final ExportService exportService;
    private final ExportCacheService exportCacheService;
    private final QueueHourlyStatsRepository statsRepository;

    private User getUserOrThrow(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
    }

    public LocalDateTime getLastQueueJoinTime(String userId) {
        User user = getUserOrThrow(userId);
        return user.getLastQueueJoinTime();
    }

    public boolean canUserJoinQueue(String userId) {
        User user = getUserOrThrow(userId);
        return user.getActiveTokenId() == null;
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

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        int nextToken = queue.getTokenCounter() + 1;
        queue.setTokenCounter(nextToken);
        // Globally unique token ID: queueId + "-T-" + counter
        String tokenId = queueId + "-T-" + String.format("%03d", nextToken);

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
        user.setActiveTokenId(tokenId);
        user.setLastQueueJoinTime(LocalDateTime.now());
        userRepository.save(user);

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

        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean canReset = requester.getRole() == Role.ADMIN ||
                (requester.getRole() == Role.PROVIDER && queue.getProviderId().equals(requesterId));

        if (!canReset) {
            throw new AccessDeniedException("You don't have permission to reset this queue");
        }

        QueueResetResponseDTO response = new QueueResetResponseDTO();

        if (resetRequest.getPreserveData() != null && resetRequest.getPreserveData()) {
            try {
                byte[] exportData = exportService.exportQueueToPdf(queue,
                        resetRequest.getReportType() != null ? resetRequest.getReportType() : "full",
                        resetRequest.getIncludeUserDetails());

                String exportId = "export-" + System.currentTimeMillis() + "-" + queueId;
                String filename = String.format("queue-reset-%s-%s.%s",
                        queue.getServiceName().replaceAll("\\s+", "-"),
                        System.currentTimeMillis(),
                        resetRequest.getReportType() != null && resetRequest.getReportType().equals("excel") ? "xlsx" : "pdf"
                );
                exportCacheService.saveExport(exportId, exportData, filename, queueId, resetRequest.getReportType(), "pdf");
                log.info("Queue data exported for queue {} with ID {}", queueId, exportId);

                response.setExportFileUrl("/export/exports/" + exportId);
            } catch (Exception e) {
                log.error("Failed to export queue data before reset: {}", e.getMessage());
                throw new RuntimeException("Failed to export queue data: " + e.getMessage());
            }
        }

        int tokensReset = queue.getTokens().size();

        Set<String> affectedUserIds = queue.getTokens().stream()
                .map(QueueToken::getUserId)
                .collect(Collectors.toSet());

        queue.getTokens().clear();
        queue.setTokenCounter(0);
        queue.setCurrentPosition(0);
        queue.setStartTime(LocalDateTime.now());

        if (queue.getStatistics() != null) {
            queue.getStatistics().setDailyUsersServed(0);
        }

        queueRepository.save(queue);
        broadcastQueueUpdate(queueId, queue);

        affectedUserIds.forEach(userId -> {
            userRepository.findById(userId).ifPresent(user -> {
                user.setActiveTokenId(null);
                user.setLastQueueJoinTime(null);
                userRepository.save(user);
            });
        });

        response.setSuccess(true);
        response.setMessage("Queue reset successfully");
        response.setTokensReset(tokensReset);

        log.info("Queue {} reset by user {}, {} tokens cleared", queueId, requesterId, tokensReset);
        return response;
    }

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
        String tokenId = queueId + "-T-" + String.format("%03d", nextToken);

        QueueToken token = new QueueToken(tokenId, userId, TokenStatus.WAITING.toString(), LocalDateTime.now());
        queue.getTokens().add(token);

        User user = getUserOrThrow(userId);
        user.setActiveTokenId(tokenId);
        user.setLastQueueJoinTime(LocalDateTime.now());
        userRepository.save(user);

        Queue updatedQueue = queueRepository.save(queue);
        broadcastQueueUpdate(queueId, updatedQueue);

        log.debug("Token {} added to queueId={}", tokenId, queueId);
        return token;
    }

    public QueueToken addGroupToken(String queueId, String userId, List<QueueToken.GroupMember> groupMembers) {
        Queue queue = getQueueOrThrow(queueId);

        if (!queue.getIsActive()) {
            log.warn("Inactive queue join attempt: queueId={}", queueId);
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
        String tokenId = queueId + "-G-" + String.format("%03d", nextToken);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        QueueToken token = new QueueToken(tokenId, userId, user.getName(), TokenStatus.WAITING.toString(),
                LocalDateTime.now(), groupMembers, groupMembers.size());

        queue.getTokens().add(token);
        user.setActiveTokenId(tokenId);
        user.setLastQueueJoinTime(LocalDateTime.now());
        userRepository.save(user);

        Queue updatedQueue = queueRepository.save(queue);
        broadcastQueueUpdate(queueId, updatedQueue);

        log.debug("Group token {} added to queueId={} with {} members", tokenId, queueId, groupMembers.size());
        return token;
    }

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
        String tokenId = queueId + "-E-" + String.format("%03d", nextToken);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        QueueToken token;
        if (queue.getAutoApproveEmergency()) {
            token = new QueueToken(tokenId, userId, user.getName(), TokenStatus.WAITING.toString(),
                    LocalDateTime.now(), emergencyDetails, queue.getEmergencyPriorityWeight());
            queue.getTokens().add(token);
            user.setActiveTokenId(tokenId);
            user.setLastQueueJoinTime(LocalDateTime.now());
            userRepository.save(user);
        } else {
            token = new QueueToken(tokenId, userId, user.getName(), TokenStatus.PENDING.toString(),
                    LocalDateTime.now(), emergencyDetails, queue.getEmergencyPriorityWeight());
            queue.getPendingEmergencyTokens().add(token);
        }
        Queue updatedQueue = queueRepository.save(queue);
        broadcastQueueUpdate(queueId, updatedQueue);

        log.debug("Emergency token {} added to queueId={}", tokenId, queueId);
        return token;
    }

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
            token.setStatus(TokenStatus.WAITING.toString());
            queue.getTokens().add(token);

            User user = getUserOrThrow(token.getUserId());
            user.setActiveTokenId(tokenId);
            user.setLastQueueJoinTime(LocalDateTime.now());
            userRepository.save(user);

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
        log.info("Creating queue for providerId={}, serviceName={}, placeId={}, serviceId={}",
                providerId, serviceName, placeId, serviceId);

        Queue newQueue = new Queue(providerId, serviceName, placeId, serviceId);
        return queueRepository.save(newQueue);
    }

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
        newQueue.setRequiresEmergencyApproval(requiresEmergencyApproval != null ? requiresEmergencyApproval : false);
        newQueue.setAutoApproveEmergency(autoApproveEmergency != null ? autoApproveEmergency : false);

        return queueRepository.save(newQueue);
    }

    public Queue createNewQueue(String providerId, String serviceName, String placeId, String serviceId,
                                Integer maxCapacity, Boolean supportsGroupToken, Boolean emergencySupport, Integer emergencyPriorityWeight) {
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

        return queueRepository.save(newQueue);
    }

    public List<Queue> getQueuesByProviderId(String providerId) {
        log.info("Fetching queues for providerId={}", providerId);
        List<Queue> queues = queueRepository.findByProviderId(providerId);
        queues = queues.stream()
                .filter(queue -> queue.getProviderId().equals(providerId))
                .collect(Collectors.toList());
        log.debug("Filtered to {} queues for providerId={}", queues.size(), providerId);
        return queues;
    }

    public List<Queue> getQueuesByPlaceId(String placeId) {
        log.info("Fetching queues for placeId={}", placeId);
        return queueRepository.findByPlaceId(placeId);
    }

    public List<Queue> getQueuesByServiceId(String serviceId) {
        log.info("Fetching queues for serviceId={}", serviceId);
        return queueRepository.findByServiceId(serviceId);
    }

    public Queue getQueueById(String queueId) {
        return getQueueOrThrow(queueId);
    }

    public List<Queue> getAllQueues() {
        log.info("Fetching all active queues");
        return queueRepository.findByIsActive(true);
    }

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

        if (token.getServedAt() != null) {
            long durationInMinutes = java.time.Duration.between(token.getServedAt(), token.getCompletedAt()).toMinutes();
            token.setServiceDurationMinutes(durationInMinutes);
        }

        User user = getUserOrThrow(token.getUserId());
        user.setActiveTokenId(null);
        user.setLastQueueJoinTime(null);
        userRepository.save(user);

        Queue updatedQueue = queueRepository.save(queue);
        broadcastQueueUpdate(queueId, updatedQueue);

        log.info("Token {} marked COMPLETED", tokenId);
        return updatedQueue;
    }

    @Scheduled(fixedRate = 3600000) // Run every hour
    public void cleanupExpiredTokens() {
        log.info("Cleaning up expired tokens (older than 24 hours)...");
        List<Queue> allQueues = queueRepository.findAll();

        for (Queue queue : allQueues) {
            boolean modified = false;
            Iterator<QueueToken> iterator = queue.getTokens().iterator();

            while (iterator.hasNext()) {
                QueueToken token = iterator.next();
                if (token.getIssuedAt().isBefore(LocalDateTime.now().minusHours(24))) {
                    userRepository.findById(token.getUserId()).ifPresent(user -> {
                        if (token.getTokenId().equals(user.getActiveTokenId())) {
                            user.setActiveTokenId(null);
                            user.setLastQueueJoinTime(null);
                            userRepository.save(user);
                        }
                    });
                    iterator.remove();
                    modified = true;
                    log.info("Removed expired token: {}", token.getTokenId());
                }
            }

            if (modified) {
                queueRepository.save(queue);
                broadcastQueueUpdate(queue.getId(), queue);
            }
        }
    }

    public Queue cancelToken(String queueId, String tokenId, String reason) {
        Queue queue = getQueueOrThrow(queueId);

        Optional<QueueToken> tokenToCancel = queue.getTokens().stream()
                .filter(t -> t.getTokenId().equals(tokenId))
                .findFirst();

        boolean removed = queue.getTokens().removeIf(t -> t.getTokenId().equals(tokenId));
        if (!removed) {
            removed = queue.getPendingEmergencyTokens().removeIf(t -> t.getTokenId().equals(tokenId));

            if (!removed) {
                log.error("Token not found for cancellation: {}", tokenId);
                throw new ResourceNotFoundException("Token not found with id " + tokenId);
            }
        }

        if (tokenToCancel.isPresent()) {
            QueueToken token = tokenToCancel.get();
            token.setCancellationReason(reason);

            User user = getUserOrThrow(tokenToCancel.get().getUserId());
            user.setActiveTokenId(null);
            user.setLastQueueJoinTime(null);
            userRepository.save(user);

            messagingTemplate.convertAndSendToUser(
                    token.getUserId(),
                    "/queue/token-cancelled",
                    Map.of(
                            "tokenId", tokenId,
                            "queueId", queueId,
                            "reason", reason != null ? reason : "Your token was cancelled by the provider."
                    )
            );
        }

        Queue updatedQueue = queueRepository.save(queue);
        broadcastQueueUpdate(queueId, updatedQueue);

        log.info("Token {} cancelled", tokenId);
        return updatedQueue;
    }

    public Queue reorderQueue(String queueId, List<QueueToken> newTokens) {
        Queue queue = getQueueOrThrow(queueId);
        queue.setTokens(newTokens);
        newTokens.stream()
                .filter(t -> "WAITING".equals(t.getStatus()))
                .forEach(t -> t.setNotificationSent(false));
        Queue updatedQueue = queueRepository.save(queue);
        broadcastQueueUpdate(queueId, updatedQueue);

        log.info("Queue reordered for queueId={}", queueId);
        return updatedQueue;
    }

    @PostConstruct
    public void cleanupInconsistentTokenStatuses() {
        log.info("Cleaning up inconsistent token statuses...");
        List<Queue> allQueues = queueRepository.findAll();

        for (Queue queue : allQueues) {
            boolean needsUpdate = false;

            long inServiceCount = queue.getTokens().stream()
                    .filter(t -> TokenStatus.IN_SERVICE.toString().equals(t.getStatus()))
                    .count();

            if (inServiceCount > 1) {
                log.warn("Queue {} has {} IN_SERVICE tokens, fixing...", queue.getId(), inServiceCount);

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
                log.info("Fixed inconsistent token statuses for queue {}", queue.getId());
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

        log.info("Updated statistics for queue {}", queueId);
        return updatedQueue;
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

    public Map<String, Object> getBestTimeToJoin(String queueId) {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<QueueHourlyStats> stats = statsRepository.findByQueueIdAndHourBetween(queueId, thirtyDaysAgo, LocalDateTime.now());

        Map<Integer, Double> avgByHour = stats.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getHour().getHour(),
                        Collectors.averagingInt(QueueHourlyStats::getWaitingCount)
                ));

        List<Map.Entry<Integer, Double>> sorted = avgByHour.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .limit(3)
                .collect(Collectors.toList());

        List<String> bestHours = sorted.stream()
                .map(e -> String.format("%02d:00 - %02d:00", e.getKey(), (e.getKey() + 1) % 24))
                .collect(Collectors.toList());

        List<Double> averages = sorted.stream()
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());

        return Map.of(
                "bestHours", bestHours,
                "averageWaitTimes", averages
        );
    }

    public QueueToken addTokenByType(String queueId, String userId, String tokenType) {
        if ("GROUP".equalsIgnoreCase(tokenType)) {
            throw new UnsupportedOperationException("Group tokens cannot be joined via QR");
        } else if ("EMERGENCY".equalsIgnoreCase(tokenType)) {
            return addEmergencyToken(queueId, userId, "Joined via QR");
        } else {
            return addNewToken(queueId, userId);
        }
    }


}