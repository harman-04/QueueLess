package com.queueless.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for WebSocket connection to a queue")
public class ConnectRequest {

    @NotBlank(message = "Queue ID is required")
    @Schema(description = "ID of the queue to connect to", example = "5f8d0d55b644a12a3c9a4f5b")
    private String queueId;
}