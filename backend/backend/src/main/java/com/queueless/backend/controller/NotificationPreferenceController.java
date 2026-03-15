package com.queueless.backend.controller;

import com.queueless.backend.dto.NotificationPreferenceDTO;
import com.queueless.backend.model.NotificationPreference;
import com.queueless.backend.model.Queue;
import com.queueless.backend.security.annotations.Authenticated;
import com.queueless.backend.service.NotificationPreferenceService;
import com.queueless.backend.service.QueueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/notifications/preferences")
@RequiredArgsConstructor
@Tag(name = "Notification Preferences", description = "Endpoints for managing per‑queue notification preferences")
public class NotificationPreferenceController {

    private final NotificationPreferenceService preferenceService;
    private final QueueService queueService;

    @GetMapping("/queue/{queueId}")
    @Authenticated
    @Operation(summary = "Get notification preference for a specific queue")
    public ResponseEntity<NotificationPreferenceDTO> getPreferenceForQueue(
            @PathVariable String queueId,
            Authentication authentication) {
        String userId = authentication.getName();
        NotificationPreference pref = preferenceService.getPreference(userId, queueId);
        if (pref == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(NotificationPreferenceDTO.fromEntity(pref));
    }

    @PutMapping("/queue/{queueId}")
    @Authenticated
    @Operation(summary = "Create or update notification preference for a queue")
    public ResponseEntity<NotificationPreferenceDTO> updatePreference(
            @PathVariable String queueId,
            @Valid @RequestBody NotificationPreferenceDTO dto,
            Authentication authentication) {
        String userId = authentication.getName();
        // Ensure the DTO's userId matches authenticated user (or ignore)
        dto.setUserId(userId);
        dto.setQueueId(queueId);

        NotificationPreference saved = preferenceService.createOrUpdatePreference(
                userId,
                queueId,
                dto.getNotifyBeforeMinutes(),
                dto.getNotifyOnStatusChange(),
                dto.getNotifyOnEmergencyApproval(),
                dto.getEnabled()
        );
        return ResponseEntity.ok(NotificationPreferenceDTO.fromEntity(saved));
    }

    @DeleteMapping("/queue/{queueId}")
    @Authenticated
    @Operation(summary = "Delete notification preference for a queue (revert to global defaults)")
    public ResponseEntity<?> deletePreference(
            @PathVariable String queueId,
            Authentication authentication) {
        String userId = authentication.getName();
        preferenceService.deletePreference(userId, queueId);
        return ResponseEntity.ok(Map.of("message", "Preference deleted"));
    }

    @GetMapping("/my")
    @Authenticated
    @Operation(summary = "Get all notification preferences for the authenticated user")
    public ResponseEntity<List<NotificationPreferenceDTO>> getMyPreferences(Authentication authentication) {
        String userId = authentication.getName();
        List<NotificationPreference> prefs = preferenceService.getPreferencesForUser(userId);

        // Enrich with queue names
        List<NotificationPreferenceDTO> dtos = prefs.stream()
                .map(pref -> {
                    NotificationPreferenceDTO dto = NotificationPreferenceDTO.fromEntity(pref);
                    try {
                        Queue queue = queueService.getQueueById(pref.getQueueId());
                        dto.setQueueName(queue.getServiceName());
                    } catch (Exception e) {
                        // queue might be deleted – ignore
                    }
                    return dto;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }
}