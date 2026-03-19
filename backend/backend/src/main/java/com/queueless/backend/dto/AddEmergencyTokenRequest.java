package com.queueless.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to add an emergency token")
public class AddEmergencyTokenRequest {
    @NotBlank(message = "Emergency details are required")
    @Schema(description = "Description of the emergency", example = "Severe chest pain", requiredMode = Schema.RequiredMode.REQUIRED)
    private String emergencyDetails;
}