package com.queueless.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddEmergencyTokenRequest {
    @NotBlank(message = "Emergency details are required")
    private String emergencyDetails;
}
