// src/main/java/com/queueless/backend/scheduler/AlertScheduler.java
package com.queueless.backend.scheduler;

import com.queueless.backend.model.AlertConfig;
import com.queueless.backend.model.Place;
import com.queueless.backend.model.Queue;
import com.queueless.backend.repository.AlertConfigRepository;
import com.queueless.backend.repository.PlaceRepository;
import com.queueless.backend.repository.QueueRepository;
import com.queueless.backend.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlertScheduler {

    private final AlertConfigRepository alertConfigRepository;
    private final PlaceRepository placeRepository;
    private final QueueRepository queueRepository;
    private final EmailService emailService;

    @Scheduled(fixedRate = 300000) // every 5 minutes
    public void checkThresholds() {
        log.info("Checking queue thresholds for alerts...");
        List<AlertConfig> allConfigs = alertConfigRepository.findAll();

        for (AlertConfig config : allConfigs) {
            if (!config.isEnabled()) continue;

            try {
                List<Place> adminPlaces = placeRepository.findByAdminId(config.getAdminId());
                List<String> placeIds = adminPlaces.stream().map(Place::getId).toList();
                List<Queue> queues = queueRepository.findByPlaceIdIn(placeIds);

                // Build a summary of queues exceeding threshold
                StringBuilder alertMessage = new StringBuilder();
                alertMessage.append("The following queues are currently exceeding the wait time threshold of ")
                        .append(config.getThresholdWaitTime()).append(" minutes:\n\n");

                boolean anyExceed = false;
                for (Queue queue : queues) {
                    if (queue.getEstimatedWaitTime() > config.getThresholdWaitTime()) {
                        anyExceed = true;
                        alertMessage.append("Queue: ").append(queue.getServiceName())
                                .append(" (Place: ").append(adminPlaces.stream()
                                        .filter(p -> p.getId().equals(queue.getPlaceId()))
                                        .map(Place::getName)
                                        .findFirst().orElse("Unknown"))
                                .append(") – Wait time: ").append(queue.getEstimatedWaitTime()).append(" min\n");
                    }
                }

                if (anyExceed) {
                    emailService.sendAlertEmail(config.getNotificationEmail(), alertMessage.toString());
                    log.info("Alert sent to {}", config.getNotificationEmail());
                }
            } catch (Exception e) {
                log.error("Error processing alert for admin {}: {}", config.getAdminId(), e.getMessage());
            }
        }
    }
}