package com.queueless.backend.dto;

import lombok.Data;

import java.util.Map;

@Data
public class UserDetailsResponseDTO {
    private String userId;
    private String userName;
    private String purpose;
    private String condition;
    private String notes;
    private Map<String, String> customFields;

    // Privacy flags to control what's visible
    private Boolean detailsVisible;
}