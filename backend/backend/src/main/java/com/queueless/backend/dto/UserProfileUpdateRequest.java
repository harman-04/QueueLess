package com.queueless.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "User profile update request")
public class UserProfileUpdateRequest {
    @Schema(description = "New name", example = "John Doe")
    private String name;

    @Pattern(regexp = "^\\+?[0-9\\s-]{10,15}$", message = "Phone number is not valid")
    @Schema(description = "New phone number", example = "+1234567890")
    private String phoneNumber;

    @Schema(description = "New profile image URL (or upload via separate endpoint)", example = "/uploads/user123.jpg")
    private String profileImageUrl;
}