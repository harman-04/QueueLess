// src/main/java/com/queueless/backend/model/AlertConfig.java
package com.queueless.backend.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "alert_configs")
@Data
@NoArgsConstructor
public class AlertConfig {
    @Id
    private String id;
    private String adminId;
    private int thresholdWaitTime;          // in minutes
    private String notificationEmail;        // where to send alerts (defaults to admin's email)
    private boolean enabled = true;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}