package com.queueless.backend.scheduler;

import com.queueless.backend.model.NotificationPreference;
import com.queueless.backend.model.Queue;
import com.queueless.backend.model.QueueToken;
import com.queueless.backend.model.Service;
import com.queueless.backend.model.User;
import com.queueless.backend.repository.QueueRepository;
import com.queueless.backend.repository.UserRepository;
import com.queueless.backend.service.EmailService;
import com.queueless.backend.service.FcmService;
import com.queueless.backend.service.NotificationPreferenceService;
import com.queueless.backend.service.ServiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenNotificationScheduler {

    private final QueueRepository queueRepository;
    private final UserRepository userRepository;
    private final ServiceService serviceService;
    private final EmailService emailService;
    private final FcmService fcmService;
    private final NotificationPreferenceService notificationPreferenceService;

    @Value("${app.frontend-url:https://localhost:5173}")
    private String appFrontendUrl;

    @Value("${token.notification.before-minutes:5}")
    private int notifyBeforeMinutes;

    @Scheduled(fixedRate = 60000) // every minute
    public void checkUpcomingTokens() {
        log.info("Checking for upcoming tokens to notify...");
        List<Queue> allQueues = queueRepository.findAll();

        for (Queue queue : allQueues) {
            try {
                processQueue(queue);
            } catch (Exception e) {
                log.error("Error processing queue {}: {}", queue.getId(), e.getMessage(), e);
            }
        }
    }

    private void processQueue(Queue queue) {
        if (!queue.getIsActive()) return;

        Service service = serviceService.getServiceById(queue.getServiceId());
        int avgServiceTime = service != null && service.getAverageServiceTime() != null
                ? service.getAverageServiceTime() : 5;

        List<QueueToken> waitingTokens = queue.getTokens().stream()
                .filter(t -> "WAITING".equals(t.getStatus()))
                .sorted((t1, t2) -> {
                    if (!t1.getPriority().equals(t2.getPriority())) {
                        return t2.getPriority() - t1.getPriority();
                    }
                    return t1.getIssuedAt().compareTo(t2.getIssuedAt());
                })
                .toList();

        // Fetch all notification preferences for this queue (to avoid per-user DB calls)
        Map<String, NotificationPreference> preferenceMap = notificationPreferenceService.getPreferencesForQueue(queue.getId())
                .stream()
                .collect(Collectors.toMap(NotificationPreference::getUserId, Function.identity()));

        boolean anyTokenNotified = false;
        for (int i = 0; i < waitingTokens.size(); i++) {
            QueueToken token = waitingTokens.get(i);
            if (Boolean.TRUE.equals(token.getNotificationSent())) continue;

            int estimatedMinutes = i * avgServiceTime;

            // Get user and preferences
            User user = userRepository.findById(token.getUserId()).orElse(null);
            if (user == null) continue;

            NotificationPreference pref = preferenceMap.get(user.getId());
            boolean shouldNotify = false;
            int threshold = notifyBeforeMinutes; // global default

            // Check if user has disabled notifications for this queue
            if (pref != null && pref.getEnabled() != null && !pref.getEnabled()) {
                // User disabled notifications for this queue – skip entirely
                continue;
            }

            // Use custom threshold if set
            if (pref != null && pref.getNotifyBeforeMinutes() != null) {
                threshold = pref.getNotifyBeforeMinutes();
            }

            if (estimatedMinutes <= threshold) {
                shouldNotify = true;
            }

            if (shouldNotify) {
                // Send email if enabled (global preference)
                if (user.getPreferences() != null && Boolean.TRUE.equals(user.getPreferences().getEmailNotifications())) {
                    try {
                        emailService.sendUpcomingTokenEmail(
                                user.getEmail(),
                                token.getTokenId(),
                                queue.getServiceName(),
                                estimatedMinutes,
                                queue.getId(),
                                appFrontendUrl
                        );
                        log.info("Sent email notification for token {} to {}", token.getTokenId(), user.getEmail());
                    } catch (Exception e) {
                        log.error("Failed to send email for token {}: {}", token.getTokenId(), e.getMessage());
                    }
                }

                // Send push notifications if enabled
                if (user.getPreferences() != null && Boolean.TRUE.equals(user.getPreferences().getPushNotifications())
                        && user.getFcmTokens() != null && !user.getFcmTokens().isEmpty()) {

                    String title = "Your turn is coming up!";
                    String body = String.format("Token %s for %s is about to be served (approx. %d min).",
                            token.getTokenId(), queue.getServiceName(), estimatedMinutes);
                    fcmService.sendMulticast(user.getFcmTokens(), title, body, queue.getId());
                    log.info("Sent push notifications for token {} to {} devices", token.getTokenId(), user.getFcmTokens().size());
                }

                token.setNotificationSent(true);
                anyTokenNotified = true;
            }
        }

        if (anyTokenNotified) {
            queueRepository.save(queue);
        }
    }
}