// src/main/java/com/queueless/backend/dto/JoinQrRequest.java
package com.queueless.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class JoinQrRequest {
    private String queueId;
    private String tokenType; // "REGULAR", "GROUP", "EMERGENCY" – optional, defaults to REGULAR
}