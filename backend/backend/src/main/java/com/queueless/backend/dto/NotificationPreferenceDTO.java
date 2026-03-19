package com.queueless.backend.dto;

import com.queueless.backend.model.NotificationPreference;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "Per‑queue notification preferences")
public class NotificationPreferenceDTO {
    @Schema(description = "User ID", example = "user123")
    private String userId;

    @Schema(description = "Queue ID", example = "queue123")
    private String queueId;

    @Schema(description = "Queue name (for display)", example = "Dental Checkup")
    private String queueName;

    @Min(value = 1, message = "Notification time must be at least 1 minute")
    @Schema(description = "Minutes before turn to notify", example = "5")
    private Integer notifyBeforeMinutes;

    @Schema(description = "Notify when token status changes", example = "true")
    private Boolean notifyOnStatusChange;

    @Schema(description = "Notify on emergency approval/rejection", example = "true")
    private Boolean notifyOnEmergencyApproval;

    @Schema(description = "Whether these preferences are enabled", example = "true")
    private Boolean enabled;

    @Schema(description = "Notify when queue is shortest (once per day)", example = "false")
    private Boolean notifyOnBestTime;

    public static NotificationPreferenceDTO fromEntity(NotificationPreference pref) {
        NotificationPreferenceDTO dto = new NotificationPreferenceDTO();
        dto.setUserId(pref.getUserId());
        dto.setQueueId(pref.getQueueId());
        dto.setNotifyBeforeMinutes(pref.getNotifyBeforeMinutes());
        dto.setNotifyOnStatusChange(pref.getNotifyOnStatusChange());
        dto.setNotifyOnEmergencyApproval(pref.getNotifyOnEmergencyApproval());
        dto.setEnabled(pref.getEnabled());
        dto.setNotifyOnBestTime(pref.getNotifyOnBestTime() != null ? pref.getNotifyOnBestTime() : false);
        return dto;
    }
}