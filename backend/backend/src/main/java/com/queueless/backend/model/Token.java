package com.queueless.backend.model;

import com.queueless.backend.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document("tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Token {
    @Id
    private String id;
    private String tokenValue;
    private Role role;
    private boolean isUsed;
    private LocalDateTime expiryDate;
    private String createdForEmail;
    private boolean isProviderToken;
    private String providerEmail;

    // New fields to track provider token ownership
    private String createdByAdminId; // For PROVIDER tokens: which admin purchased this token
}