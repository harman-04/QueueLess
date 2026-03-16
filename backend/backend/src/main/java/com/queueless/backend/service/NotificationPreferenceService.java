package com.queueless.backend.service;

import com.queueless.backend.exception.ResourceNotFoundException;
import com.queueless.backend.model.NotificationPreference;
import com.queueless.backend.repository.NotificationPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;
    private final QueueService queueService;
    private final UserService userService;

    public NotificationPreference getPreference(String userId, String queueId) {
        return preferenceRepository.findByUserIdAndQueueId(userId, queueId)
                .orElse(null);
    }

    public NotificationPreference createOrUpdatePreference(String userId, String queueId,
                                                           Integer notifyBeforeMinutes,
                                                           Boolean notifyOnStatusChange,
                                                           Boolean notifyOnEmergencyApproval,
                                                           Boolean enabled,
                                                           Boolean notifyOnBestTime) {
        // Validate user and queue exist
        userService.getUserById(userId);
        queueService.getQueueById(queueId);

        NotificationPreference preference = preferenceRepository.findByUserIdAndQueueId(userId, queueId)
                .orElse(new NotificationPreference());

        preference.setUserId(userId);
        preference.setQueueId(queueId);
        preference.setNotifyBeforeMinutes(notifyBeforeMinutes);
        preference.setNotifyOnStatusChange(notifyOnStatusChange);
        preference.setNotifyOnEmergencyApproval(notifyOnEmergencyApproval);
        preference.setEnabled(enabled != null ? enabled : true);
        preference.setNotifyOnBestTime(notifyOnBestTime != null ? notifyOnBestTime : false);

        if (preference.getCreatedAt() == null) {
            preference.setCreatedAt(LocalDateTime.now());
        }
        preference.setUpdatedAt(LocalDateTime.now());

        return preferenceRepository.save(preference);
    }

    public void deletePreference(String userId, String queueId) {
        preferenceRepository.deleteByUserIdAndQueueId(userId, queueId);
        log.info("Deleted notification preference for user {} on queue {}", userId, queueId);
    }

    public List<NotificationPreference> getPreferencesForUser(String userId) {
        return preferenceRepository.findByUserId(userId);
    }

    public List<NotificationPreference> getPreferencesForQueue(String queueId) {
        return preferenceRepository.findByQueueId(queueId);
    }
}