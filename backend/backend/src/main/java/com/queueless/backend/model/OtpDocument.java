package com.queueless.backend.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "otp")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpDocument {
    @Id
    private String id;

    @Indexed   // <-- added
    private String email;

    private String otp;

    @Indexed(expireAfter = "300s")   // TTL of 5 minutes – automatically deletes expired documents
    private LocalDateTime expiryTime;
}