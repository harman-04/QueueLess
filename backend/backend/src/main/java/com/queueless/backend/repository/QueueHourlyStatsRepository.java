// src/main/java/com/queueless/backend/repository/QueueHourlyStatsRepository.java
package com.queueless.backend.repository;

import com.queueless.backend.model.QueueHourlyStats;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface QueueHourlyStatsRepository extends MongoRepository<QueueHourlyStats, String> {
    List<QueueHourlyStats> findByQueueIdAndHourBetween(String queueId, LocalDateTime start, LocalDateTime end);
    void deleteByHourBefore(LocalDateTime cutoff);
}