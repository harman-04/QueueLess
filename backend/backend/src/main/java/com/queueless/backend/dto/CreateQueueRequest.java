package com.queueless.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateQueueRequest {
    @NotBlank
    private String serviceName;
    @NotBlank
    private String placeId;
    @NotBlank
    private String serviceId;
    @Min(1)
    private Integer maxCapacity;
    private Boolean supportsGroupToken;
    private Boolean emergencySupport;
    @Min(1) @Max(100)
    private Integer emergencyPriorityWeight;
    private Boolean requiresEmergencyApproval;
    private Boolean autoApproveEmergency;
}