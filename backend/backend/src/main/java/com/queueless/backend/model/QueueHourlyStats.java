// src/main/java/com/queueless/backend/model/QueueHourlyStats.java
package com.queueless.backend.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "queue_hourly_stats")
@Data
@NoArgsConstructor
public class QueueHourlyStats {
    @Id
    private String id;
    private String queueId;
    private LocalDateTime hour; // start of the hour
    private int waitingCount;
    private LocalDateTime createdAt = LocalDateTime.now();
}