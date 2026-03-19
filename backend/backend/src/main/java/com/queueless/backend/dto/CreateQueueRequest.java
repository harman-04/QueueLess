package com.queueless.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create a new queue")
public class CreateQueueRequest {
    @NotBlank
    @Schema(description = "Name of the service", example = "Dental Checkup", requiredMode = Schema.RequiredMode.REQUIRED)
    private String serviceName;

    @NotBlank
    @Schema(description = "ID of the place", example = "507f1f77bcf86cd799439011", requiredMode = Schema.RequiredMode.REQUIRED)
    private String placeId;

    @NotBlank
    @Schema(description = "ID of the service", example = "507f1f77bcf86cd799439013", requiredMode = Schema.RequiredMode.REQUIRED)
    private String serviceId;

    @Min(1)
    @Schema(description = "Maximum capacity of the queue", example = "50")
    private Integer maxCapacity;

    @Schema(description = "Whether group tokens are supported", example = "true", defaultValue = "false")
    private Boolean supportsGroupToken;

    @Schema(description = "Whether emergency tokens are supported", example = "false", defaultValue = "false")
    private Boolean emergencySupport;

    @Min(1) @Max(100)
    @Schema(description = "Priority weight for emergency tokens (higher = higher priority)", example = "10", defaultValue = "10")
    private Integer emergencyPriorityWeight;

    @Schema(description = "Whether emergency tokens require approval", example = "false", defaultValue = "false")
    private Boolean requiresEmergencyApproval;

    @Schema(description = "Whether emergency tokens are auto‑approved", example = "true", defaultValue = "false")
    private Boolean autoApproveEmergency;
}