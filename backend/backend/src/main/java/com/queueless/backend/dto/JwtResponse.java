package com.queueless.backend.dto;

import com.queueless.backend.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponse {
    private String token;
    private String role;
    private String userId;
    private String name;
    private String profileImageUrl;
    private String placeId;
    private Boolean isVerified;
    private User.UserPreferences preferences;
    private List<String> ownedPlaceIds;
}