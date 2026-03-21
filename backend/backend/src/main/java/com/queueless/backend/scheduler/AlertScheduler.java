// src/main/java/com/queueless/backend/scheduler/AlertScheduler.java
package com.queueless.backend.scheduler;

import com.queueless.backend.model.AlertConfig;
import com.queueless.backend.model.Place;
import com.queueless.backend.model.Queue;
import com.queueless.backend.model.User;
import com.queueless.backend.repository.AlertConfigRepository;
import com.queueless.backend.repository.PlaceRepository;
import com.queueless.backend.repository.QueueRepository;
import com.queueless.backend.repository.UserRepository;
import com.queueless.backend.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlertScheduler {

    private final AlertConfigRepository alertConfigRepository;
    private final PlaceRepository placeRepository;
    private final QueueRepository queueRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Value("${app.frontend-url:https://localhost:5173}")
    private String appBaseUrl;

    @Scheduled(fixedRate = 300000) // every 5 minutes
    public void checkThresholds() {
        log.info("Checking queue thresholds for alerts...");
        List<AlertConfig> allConfigs = alertConfigRepository.findAll();

        for (AlertConfig config : allConfigs) {
            if (!config.isEnabled()) continue;

            try {
                // Fetch admin user to get name
                User admin = userRepository.findById(config.getAdminId()).orElse(null);
                String adminName = (admin != null && admin.getName() != null) ? admin.getName() : "Admin";

                List<Place> adminPlaces = placeRepository.findByAdminId(config.getAdminId());
                List<String> placeIds = adminPlaces.stream().map(Place::getId).toList();
                List<Queue> queues = queueRepository.findByPlaceIdIn(placeIds);

                List<String> queueSummaries = new ArrayList<>();
                for (Queue queue : queues) {
                    if (queue.getEstimatedWaitTime() > config.getThresholdWaitTime()) {
                        String placeName = adminPlaces.stream()
                                .filter(p -> p.getId().equals(queue.getPlaceId()))
                                .map(Place::getName)
                                .findFirst()
                                .orElse("Unknown");
                        // Build HTML for a single queue item
                        String queueHtml = String.format(
                                "<div class=\"queue-item\">" +
                                        "   <div class=\"queue-name\">%s</div>" +
                                        "   <div class=\"place-name\">%s</div>" +
                                        "   <div>Wait time: <span class=\"wait-time\">%d min</span></div>" +
                                        "</div>",
                                queue.getServiceName(),
                                placeName,
                                queue.getEstimatedWaitTime()
                        );
                        queueSummaries.add(queueHtml);
                    }
                }

                if (!queueSummaries.isEmpty()) {
                    // Get appBaseUrl from configuration (inject @Value)
                    emailService.sendAlertEmail(config.getNotificationEmail(), adminName, config.getThresholdWaitTime(), queueSummaries, appBaseUrl);
                    log.info("Alert sent to {}", config.getNotificationEmail());
                }
            } catch (Exception e) {
                log.error("Error processing alert for admin {}: {}", config.getAdminId(), e.getMessage());
            }
        }
    }
}