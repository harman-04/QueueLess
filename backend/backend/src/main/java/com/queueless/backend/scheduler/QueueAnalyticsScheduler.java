// src/main/java/com/queueless/backend/scheduler/QueueAnalyticsScheduler.java
package com.queueless.backend.scheduler;

import com.queueless.backend.model.Queue;
import com.queueless.backend.model.QueueHourlyStats;
import com.queueless.backend.model.QueueToken;
import com.queueless.backend.repository.QueueHourlyStatsRepository;
import com.queueless.backend.repository.QueueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class QueueAnalyticsScheduler {

    private final QueueRepository queueRepository;
    private final QueueHourlyStatsRepository statsRepository;

    @Scheduled(cron = "0 0 * * * *") // every hour at minute 0
    public void snapshotQueueWaitCounts() {
        log.info("Taking hourly snapshot of queue waiting counts...");
        LocalDateTime hourStart = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);
        List<Queue> allQueues = queueRepository.findAll();

        for (Queue queue : allQueues) {
            long waitingCount = queue.getTokens().stream()
                    .filter(token -> "WAITING".equals(token.getStatus()))
                    .count();

            QueueHourlyStats stats = new QueueHourlyStats();
            stats.setQueueId(queue.getId());
            stats.setHour(hourStart);
            stats.setWaitingCount((int) waitingCount);
            statsRepository.save(stats);
        }

        // Delete stats older than 60 days to keep database size manageable
        LocalDateTime cutoff = LocalDateTime.now().minusDays(60);
        statsRepository.deleteByHourBefore(cutoff);
        log.info("Hourly snapshot completed");
    }
}