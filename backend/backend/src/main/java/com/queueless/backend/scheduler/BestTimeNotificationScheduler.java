package com.queueless.backend.scheduler;

import com.queueless.backend.model.NotificationPreference;
import com.queueless.backend.model.Queue;
import com.queueless.backend.model.User;
import com.queueless.backend.repository.NotificationPreferenceRepository;
import com.queueless.backend.repository.QueueRepository;
import com.queueless.backend.repository.UserRepository;
import com.queueless.backend.service.FcmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class BestTimeNotificationScheduler {

    private final QueueRepository queueRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;
    private final FcmService fcmService;

    // Threshold for "best time" – waiting tokens count below which we notify
    private static final int BEST_TIME_THRESHOLD = 3;

    @Scheduled(cron = "0 0 * * * *") // every hour at minute 0
    public void checkBestTimeNotifications() {
        log.info("Starting best time notification check...");
        List<Queue> activeQueues = queueRepository.findByIsActive(true);

        for (Queue queue : activeQueues) {
            try {
                processQueue(queue);
            } catch (Exception e) {
                log.error("Error processing queue {} for best time notifications", queue.getId(), e);
            }
        }
    }

    private void processQueue(Queue queue) {
        // Get current waiting count
        long waitingCount = queue.getTokens().stream()
                .filter(t -> "WAITING".equals(t.getStatus()))
                .count();

        // If waiting count is above threshold, nothing to do
        if (waitingCount >= BEST_TIME_THRESHOLD) {
            return;
        }

        // Get all preferences for this queue where notifyOnBestTime = true
        List<NotificationPreference> preferences = preferenceRepository.findByQueueId(queue.getId()).stream()
                .filter(p -> Boolean.TRUE.equals(p.getNotifyOnBestTime()))
                .filter(p -> Boolean.TRUE.equals(p.getEnabled())) // only if preferences are enabled
                .collect(Collectors.toList());

        if (preferences.isEmpty()) {
            return;
        }

        // Group by user for efficient user lookup
        Map<String, NotificationPreference> prefByUserId = preferences.stream()
                .collect(Collectors.toMap(NotificationPreference::getUserId, p -> p));

        // Fetch all users at once
        List<User> users = userRepository.findAllById(prefByUserId.keySet());
        LocalDateTime now = LocalDateTime.now();

        for (User user : users) {
            NotificationPreference pref = prefByUserId.get(user.getId());

            // Avoid sending too often: check last sent time (e.g., at least 24 hours apart)
            if (pref.getLastBestTimeNotificationSent() != null &&
                    pref.getLastBestTimeNotificationSent().isAfter(now.minusHours(24))) {
                continue;
            }

            // Send push notification if user has FCM tokens
            if (user.getFcmTokens() != null && !user.getFcmTokens().isEmpty()) {
                String title = "Queue is now short!";
                String body = String.format("The queue for %s currently has only %d people waiting. Great time to join!",
                        queue.getServiceName(), waitingCount);

                fcmService.sendMulticast(user.getFcmTokens(), title, body, queue.getId());

                // Update last sent time
                pref.setLastBestTimeNotificationSent(now);
                preferenceRepository.save(pref);

                log.info("Best time notification sent to user {} for queue {} (waiting {})",
                        user.getId(), queue.getId(), waitingCount);
            }
        }
    }
}