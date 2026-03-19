package com.queueless.backend.dto;

import com.queueless.backend.model.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "JWT authentication response")
public class JwtResponse {
    @Schema(description = "JWT token", example = "eyJhbGciOiJIUzI1NiIs...")
    private String token;

    @Schema(description = "User role", example = "USER")
    private String role;

    @Schema(description = "User ID", example = "507f1f77bcf86cd799439011")
    private String userId;

    @Schema(description = "User full name", example = "John Doe")
    private String name;

    @Schema(description = "Phone number", example = "+1234567890")
    private String phoneNumber;

    @Schema(description = "Profile image URL", example = "/uploads/user_123.jpg")
    private String profileImageUrl;

    @Schema(description = "Place ID (for providers)", example = "place123")
    private String placeId;

    @Schema(description = "Email verification status", example = "true")
    private Boolean isVerified;

    @Schema(description = "User preferences")
    private User.UserPreferences preferences;

    @Schema(description = "IDs of places owned by this user")
    private List<String> ownedPlaceIds;
}