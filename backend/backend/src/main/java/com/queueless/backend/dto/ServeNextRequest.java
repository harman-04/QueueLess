package com.queueless.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to serve the next token in a queue")
public class ServeNextRequest {
    @NotBlank(message = "Queue ID is required")
    @Schema(description = "ID of the queue", example = "67b1a2c3d4e5f67890123457", requiredMode = Schema.RequiredMode.REQUIRED)
    private String queueId;
}