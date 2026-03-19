package com.queueless.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "Request to join a queue via QR code")
public class JoinQrRequest {
    @NotBlank(message = "Queue ID is required")
    @Schema(description = "ID of the queue to join", example = "67b1a2c3d4e5f67890123457")
    private String queueId;

    @Schema(description = "Type of token: REGULAR, GROUP, or EMERGENCY (default REGULAR)", example = "REGULAR", allowableValues = {"REGULAR", "GROUP", "EMERGENCY"})
    private String tokenType;
}