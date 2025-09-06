package com.queueless.backend.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document("users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    private String id;

    @NotBlank
    private String name;

    @Email
    private String email;

    @NotBlank
    private String password;

    private String phoneNumber;
    private Role role;
    private String profileImageUrl;
    private String placeId; // If user is associated with a place
    private LocalDateTime createdAt;
    private Boolean isVerified;
    private UserPreferences preferences;
    private List<String> ownedPlaceIds;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserPreferences {
        private Boolean emailNotifications;
        private Boolean smsNotifications;
        private String language;
        private Integer defaultSearchRadius;
        private Boolean darkMode;
        private List<String> favoritePlaceIds;
    }
}