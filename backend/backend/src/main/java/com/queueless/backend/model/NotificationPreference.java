package com.queueless.backend.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Document(collection = "notification_preferences")
@CompoundIndexes({
        @CompoundIndex(name = "user_queue_idx", def = "{'userId': 1, 'queueId': 1}", unique = true)
})
public class NotificationPreference {
    @Id
    private String id;

    private String userId;
    private String queueId;

    // Custom notification time in minutes before turn (null = use global)
    private Integer notifyBeforeMinutes;

    // Whether to receive notifications on status changes (e.g., moved to IN_SERVICE)
    private Boolean notifyOnStatusChange;

    // Whether to receive emergency approval notifications
    private Boolean notifyOnEmergencyApproval;

    // Whether these preferences are active (user can temporarily disable)
    private Boolean enabled;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean notifyOnBestTime;
    // Add this field
    private LocalDateTime lastBestTimeNotificationSent;
}