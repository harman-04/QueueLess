package com.queueless.backend.dto;

import com.queueless.backend.enums.Role;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    private String name;

    @Email(message = "Email should be valid")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
            message = "Password must contain at least one uppercase letter, one lowercase letter, and one number")
    private String password;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[0-9\\s-]{10,15}$", message = "Phone number is not valid")
    private String phoneNumber;

    @NotNull(message = "Role is required")
    private Role role;

    private String token;
    private String placeId;
    private UserPreferences preferences;

    @Data
    public static class UserPreferences {
        private Boolean emailNotifications = true;
        private Boolean smsNotifications = false;
        private String language = "en";

        @Min(value = 1, message = "Search radius must be at least 1km")
        @Max(value = 50, message = "Search radius cannot exceed 50km")
        private Integer defaultSearchRadius = 5;

        private Boolean darkMode = false;
    }
}