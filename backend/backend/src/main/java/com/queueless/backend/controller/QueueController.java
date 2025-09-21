package com.queueless.backend.controller;

import com.queueless.backend.dto.*;
import com.queueless.backend.exception.AccessDeniedException;
import com.queueless.backend.exception.QueueInactiveException;
import com.queueless.backend.exception.ResourceNotFoundException;
import com.queueless.backend.exception.UserAlreadyInQueueException;
import com.queueless.backend.model.Queue;
import com.queueless.backend.model.QueueToken;
import com.queueless.backend.enums.Role;
import com.queueless.backend.security.annotations.AdminOrProviderOnly;
import com.queueless.backend.security.annotations.Authenticated;
import com.queueless.backend.security.annotations.ProviderOnly;
import com.queueless.backend.security.annotations.UserOnly;
import com.queueless.backend.service.QueueService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/queues")
@RequiredArgsConstructor
public class QueueController {

    private static final Logger logger = LoggerFactory.getLogger(QueueController.class);
    private final QueueService queueService;

    // ==== NEW ANNOTATION FOR USER-SPECIFIC ENDPOINTS ====
    // It's a good practice to create a @UserOnly annotation to be explicit
    // compared to the more general @Authenticated.
    // I have created and used the new @UserOnly annotation below.
    // It's similar to your existing @Authenticated but can be more specific.

    // If your @Authenticated annotation already means "hasRole('USER')",
    // you can simply use that. I will assume for clarity that you'll use
    // the new @UserOnly for user-specific endpoints.

    // ==== REQUEST DTOs ====

    /**
     * Enhanced CreateQueueRequest with emergency approval settings.
     */
    public static class CreateQueueRequest {
        private String providerId;
        private String serviceName;
        private String placeId;
        private String serviceId;
        private Integer maxCapacity;
        private Boolean supportsGroupToken;
        private Boolean emergencySupport;
        private Integer emergencyPriorityWeight;
        private Boolean requiresEmergencyApproval;
        private Boolean autoApproveEmergency;

        // Getters and setters
        public String getProviderId() { return providerId; }
        public void setProviderId(String providerId) { this.providerId = providerId; }
        public String getServiceName() { return serviceName; }
        public void setServiceName(String serviceName) { this.serviceName = serviceName; }
        public String getPlaceId() { return placeId; }
        public void setPlaceId(String placeId) { this.placeId = placeId; }
        public String getServiceId() { return serviceId; }
        public void setServiceId(String serviceId) { this.serviceId = serviceId; }
        public Integer getMaxCapacity() { return maxCapacity; }
        public void setMaxCapacity(Integer maxCapacity) { this.maxCapacity = maxCapacity; }
        public Boolean getSupportsGroupToken() { return supportsGroupToken; }
        public void setSupportsGroupToken(Boolean supportsGroupToken) { this.supportsGroupToken = supportsGroupToken; }
        public Boolean getEmergencySupport() { return emergencySupport; }
        public void setEmergencySupport(Boolean emergencySupport) { this.emergencySupport = emergencySupport; }
        public Integer getEmergencyPriorityWeight() { return emergencyPriorityWeight; }
        public void setEmergencyPriorityWeight(Integer emergencyPriorityWeight) { this.emergencyPriorityWeight = emergencyPriorityWeight; }
        public Boolean getRequiresEmergencyApproval() { return requiresEmergencyApproval; }
        public void setRequiresEmergencyApproval(Boolean requiresEmergencyApproval) { this.requiresEmergencyApproval = requiresEmergencyApproval; }
        public Boolean getAutoApproveEmergency() { return autoApproveEmergency; }
        public void setAutoApproveEmergency(Boolean autoApproveEmergency) { this.autoApproveEmergency = autoApproveEmergency; }
    }

    public static class AddGroupTokenRequest {
        private List<QueueToken.GroupMember> groupMembers;

        public List<QueueToken.GroupMember> getGroupMembers() { return groupMembers; }
        public void setGroupMembers(List<QueueToken.GroupMember> groupMembers) { this.groupMembers = groupMembers; }
    }

    public static class AddEmergencyTokenRequest {
        private String emergencyDetails;

        public String getEmergencyDetails() { return emergencyDetails; }
        public void setEmergencyDetails(String emergencyDetails) { this.emergencyDetails = emergencyDetails; }
    }

    // ==== ENDPOINTS ====

    @PostMapping("/create")
    @AdminOrProviderOnly
    public ResponseEntity<Queue> createNewQueue(@RequestBody CreateQueueRequest request) {
        // Removed manual authentication check as @AdminOrProviderOnly handles it
        try {
            String providerId = SecurityContextHolder.getContext().getAuthentication().getName();
            logger.info("Creating new queue for providerId={}, serviceName={}, placeId={}, serviceId={}, maxCapacity={}",
                    providerId, request.getServiceName(), request.getPlaceId(), request.getServiceId(), request.getMaxCapacity());

            Queue newQueue = queueService.createNewQueue(
                    providerId,
                    request.getServiceName(),
                    request.getPlaceId(),
                    request.getServiceId(),
                    request.getMaxCapacity(),
                    request.getSupportsGroupToken(),
                    request.getEmergencySupport(),
                    request.getEmergencyPriorityWeight(),
                    request.getRequiresEmergencyApproval(),
                    request.getAutoApproveEmergency()
            );
            logger.debug("Created queue: {}", newQueue);
            return new ResponseEntity<>(newQueue, HttpStatus.CREATED);
        } catch (Exception e) {
            logger.error("Error creating queue: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/by-provider")
    @ProviderOnly
    public ResponseEntity<List<Queue>> getQueuesByProviderId() {
        // Removed manual authentication check as @ProviderOnly handles it
        try {
            String providerId = SecurityContextHolder.getContext().getAuthentication().getName();
            logger.info("Fetching queues for providerId={}", providerId);
            List<Queue> queues = queueService.getQueuesByProviderId(providerId);
            logger.debug("Found {} queues for providerId={}", queues.size(), providerId);

            // Additional security check - ensure provider only sees their own queues
            queues = queues.stream()
                    .filter(queue -> queue.getProviderId().equals(providerId))
                    .collect(Collectors.toList());

            return queues.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(queues);
        } catch (Exception e) {
            logger.error("Error fetching queues by provider: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/by-place/{placeId}")
    public ResponseEntity<List<Queue>> getQueuesByPlaceId(@PathVariable String placeId) {
        try {
            logger.info("Fetching queues for placeId={}", placeId);
            List<Queue> queues = queueService.getQueuesByPlaceId(placeId);
            logger.debug("Found {} queues for placeId={}", queues.size(), placeId);
            return queues.isEmpty() ? new ResponseEntity<>(HttpStatus.NOT_FOUND) : new ResponseEntity<>(queues, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error fetching queues by place: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/by-service/{serviceId}")
    public ResponseEntity<List<Queue>> getQueuesByServiceId(@PathVariable String serviceId) {
        try {
            logger.info("Fetching queues for serviceId={}", serviceId);
            List<Queue> queues = queueService.getQueuesByServiceId(serviceId);
            logger.debug("Found {} queues for serviceId={}", queues.size(), serviceId);
            return queues.isEmpty() ? new ResponseEntity<>(HttpStatus.NOT_FOUND) : new ResponseEntity<>(queues, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error fetching queues by service: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{queueId}")
    public ResponseEntity<Queue> getQueueById(@PathVariable String queueId) {
        try {
            logger.info("Fetching queue by ID: {}", queueId);
            Queue queue = queueService.getQueueById(queueId);
            if (queue != null) {
                logger.debug("Queue found: {}", queue);
                return new ResponseEntity<>(queue, HttpStatus.OK);
            } else {
                logger.warn("Queue not found with ID: {}", queueId);
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            logger.error("Error fetching queue by ID: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/all")
    public ResponseEntity<List<Queue>> getAllQueues() {
        try {
            logger.info("Fetching all active queues for public users");
            List<Queue> queues = queueService.getAllQueues();
            logger.debug("Total active queues found: {}", queues.size());
            return new ResponseEntity<>(queues, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error fetching all queues: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/{queueId}/add-token")
    @UserOnly
    public ResponseEntity<?> addTokenToQueue(@PathVariable String queueId) {
        // Removed manual authentication check as @UserOnly handles it
        try {
            String userId = SecurityContextHolder.getContext().getAuthentication().getName();
            logger.info("Adding token to queueId={} for userId={}", queueId, userId);
            QueueToken newToken = queueService.addNewToken(queueId, userId);
            logger.debug("Token created: {}", newToken);
            return new ResponseEntity<>(newToken, HttpStatus.CREATED);
        } catch (UserAlreadyInQueueException e) {
            logger.error("User already in queue: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        } catch (QueueInactiveException e) {
            logger.error("Queue inactive: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error adding token: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Cannot join queue at this time"));
        }
    }

    @PostMapping("/{queueId}/add-group-token")
    @UserOnly
    public ResponseEntity<QueueToken> addGroupTokenToQueue(@PathVariable String queueId, @RequestBody AddGroupTokenRequest request) {
        // Removed manual authentication check as @UserOnly handles it
        try {
            String userId = SecurityContextHolder.getContext().getAuthentication().getName();
            logger.info("Adding group token to queueId={} for userId={}", queueId, userId);
            QueueToken newToken = queueService.addGroupToken(queueId, userId, request.getGroupMembers());
            logger.debug("Group token created: {}", newToken);
            return new ResponseEntity<>(newToken, HttpStatus.CREATED);
        } catch (Exception e) {
            logger.error("Error adding group token: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/{queueId}/add-emergency-token")
    @UserOnly
    public ResponseEntity<QueueToken> addEmergencyTokenToQueue(@PathVariable String queueId, @RequestBody AddEmergencyTokenRequest request) {
        // Removed manual authentication check as @UserOnly handles it
        try {
            String userId = SecurityContextHolder.getContext().getAuthentication().getName();
            logger.info("Adding emergency token to queueId={} for userId={}", queueId, userId);
            QueueToken newToken = queueService.addEmergencyToken(queueId, userId, request.getEmergencyDetails());
            logger.debug("Emergency token created: {}", newToken);
            return new ResponseEntity<>(newToken, HttpStatus.CREATED);
        } catch (Exception e) {
            logger.error("Error adding emergency token: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/{queueId}/serve-next")
    @AdminOrProviderOnly
    public ResponseEntity<Queue> serveNextToken(@PathVariable String queueId) {
        // Removed manual authentication check as @AdminOrProviderOnly handles it
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String providerId = authentication.getName();
        logger.info("Serving next token for queueId={} by providerId={}", queueId, providerId);

        Queue queue = queueService.getQueueById(queueId);
        if (queue == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        if (!queue.getProviderId().equals(providerId) &&
                authentication.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        Queue updatedQueue = queueService.serveNextToken(queueId);
        logger.debug("Updated queue after serving: {}", updatedQueue);
        return updatedQueue != null ? new ResponseEntity<>(updatedQueue, HttpStatus.OK) : new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @PostMapping("/{queueId}/complete-token")
    @AdminOrProviderOnly
    public ResponseEntity<Queue> completeToken(@PathVariable String queueId, @RequestBody TokenRequest tokenRequest) {
        // Removed manual authentication check as @AdminOrProviderOnly handles it
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String providerId = authentication.getName();
        logger.info("Completing tokenId={} in queueId={} by providerId={}", tokenRequest.getTokenId(), queueId, providerId);

        Queue queue = queueService.getQueueById(queueId);
        if (queue == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        if (!queue.getProviderId().equals(providerId) &&
                authentication.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

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
    @Authenticated
    public ResponseEntity<Queue> cancelToken(@PathVariable String queueId, @PathVariable String tokenId) {
        // Removed manual authentication check as @Authenticated handles it
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        logger.info("Cancelling tokenId={} from queueId={} by userId={}", tokenId, queueId, userId);

        Queue queue = queueService.getQueueById(queueId);
        if (queue == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        boolean isTokenOwner = queue.getTokens().stream()
                .anyMatch(t -> t.getTokenId().equals(tokenId) && t.getUserId().equals(userId));

        boolean isProviderOrAdmin = queue.getProviderId().equals(userId) ||
                authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isTokenOwner && !isProviderOrAdmin) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        Queue updatedQueue = queueService.cancelToken(queueId, tokenId);
        logger.debug("Queue after cancellation: {}", updatedQueue);
        return ResponseEntity.ok(updatedQueue);
    }

    @PutMapping("/{queueId}/activate")
    @AdminOrProviderOnly
    public ResponseEntity<Queue> activateQueue(@PathVariable String queueId) {
        // Removed manual authentication check as @AdminOrProviderOnly handles it
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String providerId = authentication.getName();
        logger.info("Activating queueId={} by providerId={}", queueId, providerId);

        Queue queue = queueService.getQueueById(queueId);
        if (queue == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        if (!queue.getProviderId().equals(providerId) &&
                authentication.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        Queue updatedQueue = queueService.setQueueActiveStatus(queueId, true);
        logger.debug("Queue activated: {}", updatedQueue);
        return ResponseEntity.ok(updatedQueue);
    }

    @PutMapping("/{queueId}/deactivate")
    @AdminOrProviderOnly
    public ResponseEntity<Queue> deactivateQueue(@PathVariable String queueId) {
        // Removed manual authentication check as @AdminOrProviderOnly handles it
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String providerId = authentication.getName();
        logger.info("Deactivating queueId={} by providerId={}", queueId, providerId);

        Queue queue = queueService.getQueueById(queueId);
        if (queue == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        if (!queue.getProviderId().equals(providerId) &&
                authentication.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        Queue updatedQueue = queueService.setQueueActiveStatus(queueId, false);
        logger.debug("Queue deactivated: {}", updatedQueue);
        return ResponseEntity.ok(updatedQueue);
    }

    @PutMapping("/{queueId}/reorder")
    @AdminOrProviderOnly
    public ResponseEntity<Queue> reorderQueue(
            @PathVariable String queueId,
            @RequestBody List<QueueToken> newTokens
    ) {
        // Removed manual authentication check as @AdminOrProviderOnly handles it
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String providerId = authentication.getName();
        logger.info("Reordering tokens in queueId={} by providerId={}", queueId, providerId);

        Queue queue = queueService.getQueueById(queueId);
        if (queue == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        if (!queue.getProviderId().equals(providerId) &&
                authentication.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        try {
            Queue updatedQueue = queueService.reorderQueue(queueId, newTokens);
            return ResponseEntity.ok(updatedQueue);
        } catch (Exception e) {
            logger.error("Error reordering queue {}: {}", queueId, e.getMessage());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/by-user/{userId}")
    @UserOnly
    public ResponseEntity<List<Queue>> getQueuesByUserId(@PathVariable String userId) {
        // The check for userId match is still necessary for security.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!authentication.getName().equals(userId)) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        try {
            logger.info("Fetching queues for userId={}", userId);
            List<Queue> queues = queueService.getQueuesByUserId(userId);
            logger.debug("Found {} queues for userId={}", queues.size(), userId);
            return queues.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(queues);
        } catch (Exception e) {
            logger.error("Error fetching queues by user: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/user/{userId}/restriction")
    @UserOnly
    public ResponseEntity<UserQueueRestrictionDTO> checkUserQueueRestriction(@PathVariable String userId) {
        // The check for userId match is still necessary for security.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!authentication.getName().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        LocalDateTime lastJoinTime = queueService.getLastQueueJoinTime(userId);
        boolean canJoin = queueService.canUserJoinQueue(userId);
        LocalDateTime canJoinAfter = lastJoinTime != null ?
                lastJoinTime.plusMinutes(queueService.getJoinCooldownMinutes()) : null;

        UserQueueRestrictionDTO restriction = new UserQueueRestrictionDTO(
                userId,
                canJoin,
                lastJoinTime,
                canJoinAfter,
                canJoin ? null : "You can only join one queue at a time. Please complete or cancel your current queue participation."
        );

        return ResponseEntity.ok(restriction);
    }

    @GetMapping("/{queueId}/pending-emergency")
    @AdminOrProviderOnly
    public ResponseEntity<List<QueueToken>> getPendingEmergencyTokens(@PathVariable String queueId) {
        try {
            List<QueueToken> pendingTokens = queueService.getPendingEmergencyTokens(queueId);
            return ResponseEntity.ok(pendingTokens);
        } catch (Exception e) {
            logger.error("Error fetching pending emergency tokens: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{queueId}/approve-emergency/{tokenId}")
    @AdminOrProviderOnly
    public ResponseEntity<Queue> approveEmergencyToken(
            @PathVariable String queueId,
            @PathVariable String tokenId,
            @RequestParam boolean approve,
            @RequestParam(required = false) String reason) {
        try {
            Queue updatedQueue = queueService.approveEmergencyToken(queueId, tokenId, approve, reason);
            return ResponseEntity.ok(updatedQueue);
        } catch (Exception e) {
            logger.error("Error approving emergency token: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{queueId}/add-token-with-details")
    @UserOnly
    public ResponseEntity<?> addTokenWithDetails(
            @PathVariable String queueId,
            @RequestBody TokenRequestDTO tokenRequest) {
        // Removed manual authentication check as @UserOnly handles it
        try {
            String userId = SecurityContextHolder.getContext().getAuthentication().getName();
            logger.info("Adding token with details to queueId={} for userId={}", queueId, userId);
            QueueToken newToken = queueService.addNewTokenWithDetails(queueId, userId, tokenRequest);
            logger.debug("Token with details created: {}", newToken);
            return new ResponseEntity<>(newToken, HttpStatus.CREATED);
        } catch (UserAlreadyInQueueException e) {
            logger.error("User already in queue: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        } catch (QueueInactiveException e) {
            logger.error("Queue inactive: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error adding token with details: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Cannot join queue at this time"));
        }
    }

    @GetMapping("/{queueId}/token/{tokenId}/user-details")
    @Authenticated
    public ResponseEntity<UserDetailsResponseDTO> getUserDetailsForToken(
            @PathVariable String queueId,
            @PathVariable String tokenId) {
        // Removed manual authentication check as @Authenticated handles it
        try {
            String requesterId = SecurityContextHolder.getContext().getAuthentication().getName();
            Role requesterRole = Role.valueOf(SecurityContextHolder.getContext().getAuthentication().getAuthorities().iterator().next().getAuthority().replace("ROLE_", ""));

            UserDetailsResponseDTO userDetails = queueService.getUserDetailsForToken(
                    queueId, tokenId, requesterId, requesterRole);

            return ResponseEntity.ok(userDetails);
        } catch (ResourceNotFoundException e) {
            logger.error("Token not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (AccessDeniedException e) {
            logger.error("Access denied: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            logger.error("Error fetching user details: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{queueId}/reset-with-options")
    @AdminOrProviderOnly
    public ResponseEntity<QueueResetResponseDTO> resetQueueWithOptions(
            @PathVariable String queueId,
            @RequestBody QueueResetRequestDTO resetRequest) {
        // Removed manual authentication check as @AdminOrProviderOnly handles it
        try {
            String requesterId = SecurityContextHolder.getContext().getAuthentication().getName();
            QueueResetResponseDTO response = queueService.resetQueueWithOptions(queueId, resetRequest, requesterId);
            return ResponseEntity.ok(response);
        } catch (AccessDeniedException e) {
            logger.error("Access denied for queue reset: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            logger.error("Error resetting queue: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}