package com.queueless.backend.dto;

import lombok.Data;
import jakarta.validation.constraints.Pattern;

@Data
public class UserProfileUpdateRequest {
    private String name;

    @Pattern(regexp = "^\\+?[0-9\\s-]{10,15}$", message = "Phone number is not valid")
    private String phoneNumber;

    private String profileImageUrl;
}