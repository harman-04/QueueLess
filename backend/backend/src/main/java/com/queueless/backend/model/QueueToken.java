package com.queueless.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class QueueToken {

    private String tokenId; // E.g., "A-001" or a unique ID

    private String userId; // The ID of the user who booked the token

    private String status; // "waiting", "in-service", "served"

    private LocalDateTime issuedAt;

    // Getters and setters
}