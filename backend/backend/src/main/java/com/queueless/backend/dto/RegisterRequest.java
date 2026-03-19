package com.queueless.backend.dto;

import com.queueless.backend.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@Schema(description = "Registration request payload")
public class RegisterRequest {
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    @Schema(description = "Full name of the user", example = "John Doe", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Email(message = "Email should be valid")
    @NotBlank(message = "Email is required")
    @Schema(description = "Email address", example = "john@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
            message = "Password must contain at least one uppercase letter, one lowercase letter, and one number")
    @Schema(description = "Password (min 8 chars, with uppercase, lowercase and number)", example = "Password123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[0-9\\s-]{10,15}$", message = "Phone number is not valid")
    @Schema(description = "Phone number", example = "+1234567890", requiredMode = Schema.RequiredMode.REQUIRED)
    private String phoneNumber;

    @NotNull(message = "Role is required")
    @Schema(description = "User role", example = "USER", requiredMode = Schema.RequiredMode.REQUIRED)
    private Role role;

    @Schema(description = "Registration token (required for ADMIN or PROVIDER)", example = "ADMIN-abc123")
    private String token;

    @Schema(description = "Place ID (only for PROVIDER)", example = "place123")
    private String placeId;

    @Valid
    @Schema(description = "User preferences (optional)")
    private UserPreferences preferences;

    @Data
    @Schema(description = "User preferences")
    public static class UserPreferences {
        @Schema(description = "Receive email notifications", example = "true", defaultValue = "true")
        private Boolean emailNotifications = true;

        @Schema(description = "Receive SMS notifications", example = "false", defaultValue = "false")
        private Boolean smsNotifications = false;

        @Schema(description = "Receive push notifications", example = "true", defaultValue = "true")
        private Boolean pushNotifications = true;

        @Size(min = 2, max = 10, message = "Language code must be between 2 and 10 characters")
        @Schema(description = "Language preference", example = "en", defaultValue = "en")
        private String language = "en";

        @Min(value = 1, message = "Search radius must be at least 1km")
        @Max(value = 50, message = "Search radius cannot exceed 50km")
        @Schema(description = "Default search radius in km", example = "5", defaultValue = "5")
        private Integer defaultSearchRadius = 5;

        @Schema(description = "Dark mode preference", example = "false", defaultValue = "false")
        private Boolean darkMode = false;
    }
}