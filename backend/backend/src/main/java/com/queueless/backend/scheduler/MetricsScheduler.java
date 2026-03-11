package com.queueless.backend.scheduler;

import com.queueless.backend.service.QueueMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MetricsScheduler {

    private final QueueMetricsService queueMetricsService;

    @Scheduled(fixedDelay = 15000) // update every 15 seconds
    public void updateQueueMetrics() {
        log.debug("Updating queue metrics");
        queueMetricsService.updateAllMetrics();
    }
}