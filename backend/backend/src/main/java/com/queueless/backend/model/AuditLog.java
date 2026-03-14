package com.queueless.backend.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@Document(collection = "audit_logs")
public class AuditLog {
    @Id
    private String id;

    @Indexed
    private String userId;          // who performed the action (null for unauthenticated)

    private String action;           // e.g., "QUEUE_JOIN", "TOKEN_SERVED"

    private String description;      // human-readable description

    private Map<String, Object> details; // additional structured data

    @Indexed
    private LocalDateTime timestamp;
}