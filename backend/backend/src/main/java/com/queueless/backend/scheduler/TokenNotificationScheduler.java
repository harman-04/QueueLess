// src/main/java/com/queueless/backend/scheduler/TokenNotificationScheduler.java
package com.queueless.backend.scheduler;

import com.queueless.backend.model.Queue;
import com.queueless.backend.model.QueueToken;
import com.queueless.backend.model.Service;
import com.queueless.backend.model.User;
import com.queueless.backend.repository.QueueRepository;
import com.queueless.backend.repository.UserRepository;
import com.queueless.backend.service.EmailService;
import com.queueless.backend.service.ServiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenNotificationScheduler {

    private final QueueRepository queueRepository;
    private final UserRepository userRepository;
    private final ServiceService serviceService;
    private final EmailService emailService;

    private static final int NOTIFY_BEFORE_MINUTES = 5; // threshold in minutes

    @Scheduled(fixedRate = 60000) // every minute
    public void checkUpcomingTokens() {
        log.info("Checking for upcoming tokens to notify...");
        List<Queue> allQueues = queueRepository.findAll();

        for (Queue queue : allQueues) {
            if (!queue.getIsActive()) continue; // skip inactive queues

            // Get service to know average service time
            Service service = serviceService.getServiceById(queue.getServiceId());
            int avgServiceTime = service != null && service.getAverageServiceTime() != null
                    ? service.getAverageServiceTime() : 5;

            // Get list of waiting tokens in order (sorted by priority and then issue time)
            List<QueueToken> waitingTokens = queue.getTokens().stream()
                    .filter(t -> "WAITING".equals(t.getStatus()))
                    .sorted((t1, t2) -> {
                        // Higher priority first, then earlier issuedAt
                        if (!t1.getPriority().equals(t2.getPriority())) {
                            return t2.getPriority() - t1.getPriority();
                        }
                        return t1.getIssuedAt().compareTo(t2.getIssuedAt());
                    })
                    .toList();

            for (int i = 0; i < waitingTokens.size(); i++) {
                QueueToken token = waitingTokens.get(i);
                // Skip if already notified
                if (Boolean.TRUE.equals(token.getNotificationSent())) continue;

                // Estimate remaining wait time: (i) * avgServiceTime minutes
                int estimatedMinutes = i * avgServiceTime;

                if (estimatedMinutes <= NOTIFY_BEFORE_MINUTES) {
                    // User is about to be served – send notification
                    User user = userRepository.findById(token.getUserId()).orElse(null);
                    if (user != null && user.getPreferences() != null
                            && Boolean.TRUE.equals(user.getPreferences().getEmailNotifications())) {
                        try {
                            emailService.sendUpcomingTokenEmail(user.getEmail(), token.getTokenId(), queue.getServiceName(), estimatedMinutes);
                            token.setNotificationSent(true);
                            log.info("Sent upcoming notification for token {} to {}", token.getTokenId(), user.getEmail());
                        } catch (Exception e) {
                            log.error("Failed to send email for token {}: {}", token.getTokenId(), e.getMessage());
                        }
                    }
                }
            }

            // Save queue only if any token was modified
            if (waitingTokens.stream().anyMatch(t -> Boolean.TRUE.equals(t.getNotificationSent()))) {
                queueRepository.save(queue);
            }
        }
    }
}