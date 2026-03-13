// src/main/java/com/queueless/backend/dto/JoinQrRequest.java
package com.queueless.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class JoinQrRequest {
    @NotBlank(message = "Queue ID is required")
    private String queueId;
    private String tokenType; // "REGULAR", "GROUP", "EMERGENCY" – optional, defaults to REGULAR
}