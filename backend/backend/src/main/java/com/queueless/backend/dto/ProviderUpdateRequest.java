package com.queueless.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Data
@Schema(description = "Request to update provider information")
public class ProviderUpdateRequest {
    @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    @Schema(description = "Provider's full name", example = "Dr. John Smith")
    private String name;

    @Email(message = "Email should be valid")
    @Schema(description = "Provider's email address", example = "provider@example.com")
    private String email;

    @Size(min = 10, max = 15, message = "Phone number must be 10-15 characters")
    @Schema(description = "Provider's phone number", example = "+91 9876543210")
    private String phoneNumber;

    @Schema(description = "List of place IDs this provider should manage")
    private List<String> managedPlaceIds;

    @Schema(description = "Whether the provider account is active", example = "true")
    private Boolean isActive;
}