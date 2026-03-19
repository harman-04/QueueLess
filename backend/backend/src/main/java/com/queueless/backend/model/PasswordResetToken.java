package com.queueless.backend.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Document(collection = "password_reset_tokens")
public class PasswordResetToken {
    @Id
    private String id;

    @Indexed(unique = true)
    private String token;

    private String userId;       // ID of the user (provider) this token is for
    private String email;         // snapshot of email for verification

    @Indexed(expireAfter = "24h") // Auto‑delete after 24 hours
    private LocalDateTime expiryDate;

    private boolean used;
}