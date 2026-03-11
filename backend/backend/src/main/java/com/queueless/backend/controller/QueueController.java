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
import com.queueless.backend.service.QRCodeService;
import com.queueless.backend.service.QueueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/queues")
@RequiredArgsConstructor
@Tag(name = "Queues", description = "Endpoints for managing queues and tokens")
public class QueueController {

    private final QueueService queueService;
    private final QRCodeService qrCodeService;

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
        private Boolean requiresEmergencyApproval;
        private Boolean autoApproveEmergency;

        // Getters and setters (omitted for brevity, but should be present)
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
    @Operation(summary = "Create a new queue", description = "Creates a new queue. Only provider or admin can create.")
    @ApiResponse(responseCode = "201", description = "Queue created",
            content = @Content(schema = @Schema(implementation = Queue.class)))
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public ResponseEntity<Queue> createNewQueue(@RequestBody CreateQueueRequest request) {
        try {
            String providerId = SecurityContextHolder.getContext().getAuthentication().getName();
            log.info("Creating new queue for providerId={}, serviceName={}, placeId={}, serviceId={}, maxCapacity={}",
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
            log.debug("Created queue: {}", newQueue);
            return new ResponseEntity<>(newQueue, HttpStatus.CREATED);
        } catch (Exception e) {
            log.error("Error creating queue: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/by-provider")
    @ProviderOnly
    @Operation(summary = "Get queues by provider", description = "Returns all queues managed by the authenticated provider.")
    @ApiResponse(responseCode = "200", description = "List of queues")
    @ApiResponse(responseCode = "204", description = "No queues found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public ResponseEntity<List<Queue>> getQueuesByProviderId() {
        try {
            String providerId = SecurityContextHolder.getContext().getAuthentication().getName();
            log.info("Fetching queues for providerId={}", providerId);
            List<Queue> queues = queueService.getQueuesByProviderId(providerId);
            log.debug("Found {} queues for providerId={}", queues.size(), providerId);

            queues = queues.stream()
                    .filter(queue -> queue.getProviderId().equals(providerId))
                    .collect(Collectors.toList());

            return queues.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(queues);
        } catch (Exception e) {
            log.error("Error fetching queues by provider: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/by-place/{placeId}")
    @Operation(summary = "Get queues by place", description = "Returns all queues for a specific place. Public access.")
    @ApiResponse(responseCode = "200", description = "List of queues")
    @ApiResponse(responseCode = "404", description = "No queues found")
    public ResponseEntity<List<Queue>> getQueuesByPlaceId(@PathVariable String placeId) {
        try {
            log.info("Fetching queues for placeId={}", placeId);
            List<Queue> queues = queueService.getQueuesByPlaceId(placeId);
            log.debug("Found {} queues for placeId={}", queues.size(), placeId);
            return queues.isEmpty() ? new ResponseEntity<>(HttpStatus.NOT_FOUND) : new ResponseEntity<>(queues, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error fetching queues by place: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/by-service/{serviceId}")
    @Operation(summary = "Get queues by service", description = "Returns all queues for a specific service. Public access.")
    @ApiResponse(responseCode = "200", description = "List of queues")
    @ApiResponse(responseCode = "404", description = "No queues found")
    public ResponseEntity<List<Queue>> getQueuesByServiceId(@PathVariable String serviceId) {
        try {
            log.info("Fetching queues for serviceId={}", serviceId);
            List<Queue> queues = queueService.getQueuesByServiceId(serviceId);
            log.debug("Found {} queues for serviceId={}", queues.size(), serviceId);
            return queues.isEmpty() ? new ResponseEntity<>(HttpStatus.NOT_FOUND) : new ResponseEntity<>(queues, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error fetching queues by service: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{queueId}")
    @Operation(summary = "Get queue by ID", description = "Returns a single queue by its ID. Public access.")
    @ApiResponse(responseCode = "200", description = "Queue found",
            content = @Content(schema = @Schema(implementation = Queue.class)))
    @ApiResponse(responseCode = "404", description = "Queue not found")
    public ResponseEntity<Queue> getQueueById(@PathVariable String queueId) {
        try {
            log.info("Fetching queue by ID: {}", queueId);
            Queue queue = queueService.getQueueById(queueId);
            if (queue != null) {
                log.debug("Queue found: {}", queue);
                return new ResponseEntity<>(queue, HttpStatus.OK);
            } else {
                log.warn("Queue not found with ID: {}", queueId);
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            log.error("Error fetching queue by ID: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/all")
    @Operation(summary = "Get all active queues", description = "Returns all active queues for public viewing.")
    @ApiResponse(responseCode = "200", description = "List of active queues")
    public ResponseEntity<List<Queue>> getAllQueues() {
        try {
            log.info("Fetching all active queues for public users");
            List<Queue> queues = queueService.getAllQueues();
            log.debug("Total active queues found: {}", queues.size());
            return new ResponseEntity<>(queues, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error fetching all queues: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/{queueId}/add-token")
    @UserOnly
    @Operation(summary = "Add a regular token", description = "Adds a regular token to the queue for the authenticated user.")
    @ApiResponse(responseCode = "201", description = "Token created",
            content = @Content(schema = @Schema(implementation = QueueToken.class)))
    @ApiResponse(responseCode = "400", description = "Bad request (e.g., queue inactive)")
    @ApiResponse(responseCode = "409", description = "Conflict (user already in queue)")
    public ResponseEntity<?> addTokenToQueue(@PathVariable String queueId) {
        try {
            String userId = SecurityContextHolder.getContext().getAuthentication().getName();
            log.info("Adding token to queueId={} for userId={}", queueId, userId);
            QueueToken newToken = queueService.addNewToken(queueId, userId);
            log.debug("Token created: {}", newToken);
            return new ResponseEntity<>(newToken, HttpStatus.CREATED);
        } catch (UserAlreadyInQueueException e) {
            log.error("User already in queue: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        } catch (QueueInactiveException e) {
            log.error("Queue inactive: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error adding token: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Cannot join queue at this time"));
        }
    }

    @PostMapping("/{queueId}/add-group-token")
    @UserOnly
    @Operation(summary = "Add a group token", description = "Adds a group token to the queue. Requires group members.")
    @ApiResponse(responseCode = "201", description = "Group token created",
            content = @Content(schema = @Schema(implementation = QueueToken.class)))
    @ApiResponse(responseCode = "400", description = "Bad request")
    public ResponseEntity<QueueToken> addGroupTokenToQueue(@PathVariable String queueId, @RequestBody AddGroupTokenRequest request) {
        try {
            String userId = SecurityContextHolder.getContext().getAuthentication().getName();
            log.info("Adding group token to queueId={} for userId={}", queueId, userId);
            QueueToken newToken = queueService.addGroupToken(queueId, userId, request.getGroupMembers());
            log.debug("Group token created: {}", newToken);
            return new ResponseEntity<>(newToken, HttpStatus.CREATED);
        } catch (Exception e) {
            log.error("Error adding group token: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/{queueId}/add-emergency-token")
    @UserOnly
    @Operation(summary = "Add an emergency token", description = "Adds an emergency token to the queue. Requires emergency details.")
    @ApiResponse(responseCode = "201", description = "Emergency token created",
            content = @Content(schema = @Schema(implementation = QueueToken.class)))
    @ApiResponse(responseCode = "400", description = "Bad request")
    public ResponseEntity<QueueToken> addEmergencyTokenToQueue(@PathVariable String queueId, @RequestBody AddEmergencyTokenRequest request) {
        try {
            String userId = SecurityContextHolder.getContext().getAuthentication().getName();
            log.info("Adding emergency token to queueId={} for userId={}", queueId, userId);
            QueueToken newToken = queueService.addEmergencyToken(queueId, userId, request.getEmergencyDetails());
            log.debug("Emergency token created: {}", newToken);
            return new ResponseEntity<>(newToken, HttpStatus.CREATED);
        } catch (Exception e) {
            log.error("Error adding emergency token: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/{queueId}/serve-next")
    @AdminOrProviderOnly
    @Operation(summary = "Serve next token", description = "Moves the next waiting token to in-service status.")
    @ApiResponse(responseCode = "200", description = "Queue updated",
            content = @Content(schema = @Schema(implementation = Queue.class)))
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @ApiResponse(responseCode = "404", description = "Queue not found")
    public ResponseEntity<Queue> serveNextToken(@PathVariable String queueId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String providerId = authentication.getName();
        log.info("Serving next token for queueId={} by providerId={}", queueId, providerId);

        Queue queue = queueService.getQueueById(queueId);
        if (queue == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        if (!queue.getProviderId().equals(providerId) &&
                authentication.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        Queue updatedQueue = queueService.serveNextToken(queueId);
        log.debug("Updated queue after serving: {}", updatedQueue);
        return updatedQueue != null ? new ResponseEntity<>(updatedQueue, HttpStatus.OK) : new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @PostMapping("/{queueId}/complete-token")
    @AdminOrProviderOnly
    @Operation(summary = "Complete a token", description = "Marks an in-service token as completed.")
    @ApiResponse(responseCode = "200", description = "Queue updated",
            content = @Content(schema = @Schema(implementation = Queue.class)))
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @ApiResponse(responseCode = "404", description = "Queue or token not found")
    public ResponseEntity<Queue> completeToken(@PathVariable String queueId, @RequestBody TokenRequest tokenRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String providerId = authentication.getName();
        log.info("Completing tokenId={} in queueId={} by providerId={}", tokenRequest.getTokenId(), queueId, providerId);

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
            log.debug("Queue after completion: {}", updatedQueue);
            return new ResponseEntity<>(updatedQueue, HttpStatus.OK);
        } catch (ResourceNotFoundException e) {
            log.error("Failed to complete token: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/{queueId}/cancel-token/{tokenId}")
    @Authenticated
    @Operation(summary = "Cancel a token", description = "Cancels a token. Users can cancel their own tokens; providers/admins can cancel any.")
    @ApiResponse(responseCode = "200", description = "Queue updated",
            content = @Content(schema = @Schema(implementation = Queue.class)))
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @ApiResponse(responseCode = "404", description = "Queue or token not found")
    public ResponseEntity<Queue> cancelToken(
            @PathVariable String queueId,
            @PathVariable String tokenId,
            @RequestParam(required = false) String reason) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        log.info("Cancelling tokenId={} from queueId={} by userId={}", tokenId, queueId, userId);

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

        Queue updatedQueue = queueService.cancelToken(queueId, tokenId, reason);
        log.debug("Queue after cancellation: {}", updatedQueue);
        return ResponseEntity.ok(updatedQueue);
    }

    @PutMapping("/{queueId}/activate")
    @AdminOrProviderOnly
    @Operation(summary = "Activate queue", description = "Activates a paused queue.")
    @ApiResponse(responseCode = "200", description = "Queue activated",
            content = @Content(schema = @Schema(implementation = Queue.class)))
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @ApiResponse(responseCode = "404", description = "Queue not found")
    public ResponseEntity<Queue> activateQueue(@PathVariable String queueId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String providerId = authentication.getName();
        log.info("Activating queueId={} by providerId={}", queueId, providerId);

        Queue queue = queueService.getQueueById(queueId);
        if (queue == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        if (!queue.getProviderId().equals(providerId) &&
                authentication.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        Queue updatedQueue = queueService.setQueueActiveStatus(queueId, true);
        log.debug("Queue activated: {}", updatedQueue);
        return ResponseEntity.ok(updatedQueue);
    }

    @PutMapping("/{queueId}/deactivate")
    @AdminOrProviderOnly
    @Operation(summary = "Deactivate queue", description = "Pauses an active queue.")
    @ApiResponse(responseCode = "200", description = "Queue deactivated",
            content = @Content(schema = @Schema(implementation = Queue.class)))
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @ApiResponse(responseCode = "404", description = "Queue not found")
    public ResponseEntity<Queue> deactivateQueue(@PathVariable String queueId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String providerId = authentication.getName();
        log.info("Deactivating queueId={} by providerId={}", queueId, providerId);

        Queue queue = queueService.getQueueById(queueId);
        if (queue == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        if (!queue.getProviderId().equals(providerId) &&
                authentication.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        Queue updatedQueue = queueService.setQueueActiveStatus(queueId, false);
        log.debug("Queue deactivated: {}", updatedQueue);
        return ResponseEntity.ok(updatedQueue);
    }

    @PutMapping("/{queueId}/reorder")
    @AdminOrProviderOnly
    @Operation(summary = "Reorder queue", description = "Reorders the waiting tokens in the queue.")
    @ApiResponse(responseCode = "200", description = "Queue reordered",
            content = @Content(schema = @Schema(implementation = Queue.class)))
    @ApiResponse(responseCode = "400", description = "Bad request")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @ApiResponse(responseCode = "404", description = "Queue not found")
    public ResponseEntity<Queue> reorderQueue(
            @PathVariable String queueId,
            @RequestBody List<QueueToken> newTokens) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String providerId = authentication.getName();
        log.info("Reordering tokens in queueId={} by providerId={}", queueId, providerId);

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
            log.error("Error reordering queue {}: {}", queueId, e.getMessage());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/by-user/{userId}")
    @UserOnly
    @Operation(summary = "Get queues by user", description = "Returns queues that the user has tokens in.")
    @ApiResponse(responseCode = "200", description = "List of queues")
    @ApiResponse(responseCode = "204", description = "No queues found")
    @ApiResponse(responseCode = "403", description = "Forbidden – cannot access another user's data")
    public ResponseEntity<List<Queue>> getQueuesByUserId(@PathVariable String userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!authentication.getName().equals(userId)) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        try {
            log.info("Fetching queues for userId={}", userId);
            List<Queue> queues = queueService.getQueuesByUserId(userId);
            log.debug("Found {} queues for userId={}", queues.size(), userId);
            return queues.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(queues);
        } catch (Exception e) {
            log.error("Error fetching queues by user: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/user/{userId}/restriction")
    @UserOnly
    @Operation(summary = "Check user queue restriction", description = "Checks if the user can join a new queue.")
    @ApiResponse(responseCode = "200", description = "Restriction status")
    public ResponseEntity<UserQueueRestrictionDTO> checkUserQueueRestriction(@PathVariable String userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!authentication.getName().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        LocalDateTime lastJoinTime = queueService.getLastQueueJoinTime(userId);
        boolean canJoin = queueService.canUserJoinQueue(userId);
        LocalDateTime canJoinAfter = null;

        UserQueueRestrictionDTO restriction = new UserQueueRestrictionDTO(
                userId,
                canJoin,
                lastJoinTime,
                canJoinAfter,
                canJoin ? null : "You already have an active token in another queue. Please complete or cancel it before joining a new queue."
        );

        return ResponseEntity.ok(restriction);
    }

    @GetMapping("/{queueId}/pending-emergency")
    @AdminOrProviderOnly
    @Operation(summary = "Get pending emergency tokens", description = "Returns all pending emergency tokens for a queue.")
    @ApiResponse(responseCode = "200", description = "List of pending emergency tokens")
    public ResponseEntity<List<QueueToken>> getPendingEmergencyTokens(@PathVariable String queueId) {
        try {
            List<QueueToken> pendingTokens = queueService.getPendingEmergencyTokens(queueId);
            return ResponseEntity.ok(pendingTokens);
        } catch (Exception e) {
            log.error("Error fetching pending emergency tokens: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{queueId}/approve-emergency/{tokenId}")
    @AdminOrProviderOnly
    @Operation(summary = "Approve/reject emergency token", description = "Approves or rejects a pending emergency token.")
    @ApiResponse(responseCode = "200", description = "Queue updated",
            content = @Content(schema = @Schema(implementation = Queue.class)))
    public ResponseEntity<Queue> approveEmergencyToken(
            @PathVariable String queueId,
            @PathVariable String tokenId,
            @RequestParam boolean approve,
            @RequestParam(required = false) String reason) {
        try {
            Queue updatedQueue = queueService.approveEmergencyToken(queueId, tokenId, approve, reason);
            return ResponseEntity.ok(updatedQueue);
        } catch (Exception e) {
            log.error("Error approving emergency token: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{queueId}/add-token-with-details")
    @UserOnly
    @Operation(summary = "Add token with details", description = "Adds a token with additional user details (purpose, condition, etc.)")
    @ApiResponse(responseCode = "201", description = "Token created",
            content = @Content(schema = @Schema(implementation = QueueToken.class)))
    @ApiResponse(responseCode = "400", description = "Bad request")
    @ApiResponse(responseCode = "409", description = "Conflict (user already in queue)")
    public ResponseEntity<?> addTokenWithDetails(
            @PathVariable String queueId,
            @RequestBody TokenRequestDTO tokenRequest) {
        try {
            String userId = SecurityContextHolder.getContext().getAuthentication().getName();
            log.info("Adding token with details to queueId={} for userId={}", queueId, userId);
            QueueToken newToken = queueService.addNewTokenWithDetails(queueId, userId, tokenRequest);
            log.debug("Token with details created: {}", newToken);
            return new ResponseEntity<>(newToken, HttpStatus.CREATED);
        } catch (UserAlreadyInQueueException e) {
            log.error("User already in queue: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        } catch (QueueInactiveException e) {
            log.error("Queue inactive: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error adding token with details: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Cannot join queue at this time"));
        }
    }

    @GetMapping("/{queueId}/token/{tokenId}/user-details")
    @Authenticated
    @Operation(summary = "Get user details for a token", description = "Returns user details for a specific token, respecting privacy settings.")
    @ApiResponse(responseCode = "200", description = "User details",
            content = @Content(schema = @Schema(implementation = UserDetailsResponseDTO.class)))
    @ApiResponse(responseCode = "403", description = "Access denied")
    @ApiResponse(responseCode = "404", description = "Token not found")
    public ResponseEntity<UserDetailsResponseDTO> getUserDetailsForToken(
            @PathVariable String queueId,
            @PathVariable String tokenId) {
        try {
            String requesterId = SecurityContextHolder.getContext().getAuthentication().getName();
            Role requesterRole = Role.valueOf(SecurityContextHolder.getContext().getAuthentication().getAuthorities().iterator().next().getAuthority().replace("ROLE_", ""));

            UserDetailsResponseDTO userDetails = queueService.getUserDetailsForToken(
                    queueId, tokenId, requesterId, requesterRole);

            return ResponseEntity.ok(userDetails);
        } catch (ResourceNotFoundException e) {
            log.error("Token not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (AccessDeniedException e) {
            log.error("Access denied: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            log.error("Error fetching user details: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{queueId}/reset-with-options")
    @AdminOrProviderOnly
    @Operation(summary = "Reset queue with options", description = "Resets the queue, optionally preserving data via export.")
    @ApiResponse(responseCode = "200", description = "Queue reset",
            content = @Content(schema = @Schema(implementation = QueueResetResponseDTO.class)))
    @ApiResponse(responseCode = "403", description = "Access denied")
    public ResponseEntity<QueueResetResponseDTO> resetQueueWithOptions(
            @PathVariable String queueId,
            @RequestBody QueueResetRequestDTO resetRequest) {
        try {
            String requesterId = SecurityContextHolder.getContext().getAuthentication().getName();
            QueueResetResponseDTO response = queueService.resetQueueWithOptions(queueId, resetRequest, requesterId);
            return ResponseEntity.ok(response);
        } catch (AccessDeniedException e) {
            log.error("Access denied for queue reset: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            log.error("Error resetting queue: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{queueId}/best-time")
    @Operation(summary = "Get best time to join", description = "Analyzes historical data to suggest the best times to join the queue.")
    @ApiResponse(responseCode = "200", description = "Best time recommendations")
    public ResponseEntity<Map<String, Object>> getBestTimeToJoin(@PathVariable String queueId) {
        Map<String, Object> bestTime = queueService.getBestTimeToJoin(queueId);
        return ResponseEntity.ok(bestTime);
    }

    @GetMapping("/{queueId}/qr")
    @AdminOrProviderOnly
    public ResponseEntity<byte[]> generateQueueQR(@PathVariable String queueId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String providerId = authentication.getName();

        Queue queue = queueService.getQueueById(queueId);
        if (queue == null) {
            return ResponseEntity.notFound().build();
        }

        // Check if the requester is the provider of this queue
        boolean isProvider = queue.getProviderId().equals(providerId);
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isProvider && !isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Generate QR code containing a JSON object with queueId and default token type
        String qrContent = String.format("{\"queueId\":\"%s\",\"tokenType\":\"REGULAR\"}", queueId);
        byte[] qrImage = qrCodeService.generateQRCode(qrContent, 300, 300);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=queue-qr.png")
                .contentType(MediaType.IMAGE_PNG)
                .body(qrImage);
    }

    @PostMapping("/join-by-qr")
    @Authenticated
    @Operation(summary = "Join queue via QR scan", description = "Directly adds a token to the queue from a scanned QR code.")
    @ApiResponse(responseCode = "201", description = "Token created")
    @ApiResponse(responseCode = "400", description = "Invalid request or queue inactive")
    @ApiResponse(responseCode = "409", description = "User already in queue")
    public ResponseEntity<?> joinByQr(@RequestBody JoinQrRequest request) {
        try {
            String userId = SecurityContextHolder.getContext().getAuthentication().getName();
            QueueToken token = queueService.addTokenByType(request.getQueueId(), userId, request.getTokenType());
            return new ResponseEntity<>(token, HttpStatus.CREATED);
        } catch (UserAlreadyInQueueException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        } catch (QueueInactiveException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{queueId}/position/{userId}")
    @Authenticated
    @Operation(summary = "Get user position in queue")
    @ApiResponse(responseCode = "200", description = "Position info",
            content = @Content(schema = @Schema(implementation = UserPositionDTO.class)))
    public ResponseEntity<UserPositionDTO> getUserPosition(
            @PathVariable String queueId,
            @PathVariable String userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!authentication.getName().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        UserPositionDTO position = queueService.getUserPosition(queueId, userId);
        return ResponseEntity.ok(position);
    }
}