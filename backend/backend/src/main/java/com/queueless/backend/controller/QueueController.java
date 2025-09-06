package com.queueless.backend.controller;

import com.queueless.backend.dto.TokenRequest;
import com.queueless.backend.exception.QueueInactiveException;
import com.queueless.backend.exception.ResourceNotFoundException;
import com.queueless.backend.exception.UserAlreadyInQueueException;
import com.queueless.backend.model.Queue;
import com.queueless.backend.model.QueueToken;
import com.queueless.backend.service.QueueService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/queues")
@RequiredArgsConstructor
public class QueueController {

    private static final Logger logger = LoggerFactory.getLogger(QueueController.class);
    private final QueueService queueService;

    // ==== REQUEST DTOs ====

    public static class CreateQueueRequest {
        private String providerId;
        private String serviceName;
        private String placeId;
        private String serviceId;
        private Integer maxCapacity;
        private Boolean supportsGroupToken;
        private Boolean emergencySupport;
        private Integer emergencyPriorityWeight;


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
    @PreAuthorize("hasAnyRole('PROVIDER', 'ADMIN')")
    public ResponseEntity<Queue> createNewQueue(@RequestBody CreateQueueRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            String providerId = authentication.getName();
            // This is the line that needs to be updated
            logger.info("Creating new queue for providerId={}, serviceName={}, placeId={}, serviceId={}, maxCapacity={}",
                    providerId, request.getServiceName(), request.getPlaceId(), request.getServiceId(), request.getMaxCapacity());

            Queue newQueue = queueService.createNewQueue(
                    providerId,
                    request.getServiceName(),
                    request.getPlaceId(),
                    request.getServiceId(),
                    request.getMaxCapacity(), // Pass the new parameters
                    request.getSupportsGroupToken(),
                    request.getEmergencySupport(),
                    request.getEmergencyPriorityWeight()
            );
            logger.debug("Created queue: {}", newQueue);
            return new ResponseEntity<>(newQueue, HttpStatus.CREATED);
        } catch (Exception e) {
            logger.error("Error creating queue: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/by-provider")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<List<Queue>> getQueuesByProviderId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            String providerId = authentication.getName();
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

    // In QueueController.java - Update the addTokenToQueue method
    @PostMapping("/{queueId}/add-token")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> addTokenToQueue(@PathVariable String queueId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            logger.error("Authentication is null for addTokenToQueue");
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        try {
            String userId = authentication.getName();
            logger.info("Adding token to queueId={} for userId={}", queueId, userId);
            QueueToken newToken = queueService.addNewToken(queueId, userId);
            logger.debug("Token created: {}", newToken);
            return new ResponseEntity<>(newToken, HttpStatus.CREATED);
        } catch (UserAlreadyInQueueException e) {
            logger.error("User already in queue: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", e.getMessage()));
        } catch (QueueInactiveException e) {
            logger.error("Queue inactive: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error adding token: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Cannot join queue at this time"));
        }
    }

    @PostMapping("/{queueId}/add-group-token")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<QueueToken> addGroupTokenToQueue(@PathVariable String queueId, @RequestBody AddGroupTokenRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            logger.error("Authentication is null for addGroupTokenToQueue");
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        try {
            String userId = authentication.getName();
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
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<QueueToken> addEmergencyTokenToQueue(@PathVariable String queueId, @RequestBody AddEmergencyTokenRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            logger.error("Authentication is null for addEmergencyTokenToQueue");
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        try {
            String userId = authentication.getName();
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
    @PreAuthorize("hasAnyRole('PROVIDER', 'ADMIN')")
    public ResponseEntity<Queue> serveNextToken(@PathVariable String queueId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

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
    @PreAuthorize("hasAnyRole('PROVIDER', 'ADMIN')")
    public ResponseEntity<Queue> completeToken(@PathVariable String queueId, @RequestBody TokenRequest tokenRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

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
    @PreAuthorize("hasAnyRole('USER', 'PROVIDER', 'ADMIN')")
    public ResponseEntity<Queue> cancelToken(@PathVariable String queueId, @PathVariable String tokenId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

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
    @PreAuthorize("hasAnyRole('PROVIDER', 'ADMIN')")
    public ResponseEntity<Queue> activateQueue(@PathVariable String queueId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

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
    @PreAuthorize("hasAnyRole('PROVIDER', 'ADMIN')")
    public ResponseEntity<Queue> deactivateQueue(@PathVariable String queueId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

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
    @PreAuthorize("hasAnyRole('PROVIDER','ADMIN')")
    public ResponseEntity<Queue> reorderQueue(
            @PathVariable String queueId,
            @RequestBody List<QueueToken> newTokens
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

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
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<Queue>> getQueuesByUserId(@PathVariable String userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.getName().equals(userId)) {
            // Ensure the user can only fetch their own queues
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

}