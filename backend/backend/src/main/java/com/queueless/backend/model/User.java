package com.queueless.backend.model;

import com.queueless.backend.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
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
    @Indexed(unique = true)   // <-- added
    private String email;

    @NotBlank
    private String password;

    private String phoneNumber;
    private Role role;
    private String profileImageUrl;
    private String placeId;
    private LocalDateTime createdAt;
    private Boolean isVerified;
    private UserPreferences preferences;
    private List<String> ownedPlaceIds;

    @Field("activeTokenId")
    private String activeTokenId;

    @Field("lastQueueJoinTime")
    private LocalDateTime lastQueueJoinTime;

    private String adminId;
    private List<String> managedPlaceIds;

    // In User.java
    @Field("fcmTokens")
    private List<String> fcmTokens;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserPreferences {
        private Boolean emailNotifications;
        private Boolean smsNotifications;
        private Boolean pushNotifications;
        private String language;
        private Integer defaultSearchRadius;
        private Boolean darkMode;
        private List<String> favoritePlaceIds;
    }
}