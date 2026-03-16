package com.queueless.backend.dto;

import com.queueless.backend.model.NotificationPreference;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class NotificationPreferenceDTO {
    private String userId;
    private String queueId;
    private String queueName; // optional, for display

    @Min(value = 1, message = "Notification time must be at least 1 minute")
    private Integer notifyBeforeMinutes;

    private Boolean notifyOnStatusChange;
    private Boolean notifyOnEmergencyApproval;
    private Boolean enabled;
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