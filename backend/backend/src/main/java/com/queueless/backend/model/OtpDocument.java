package com.queueless.backend.model;

import lombok.*;
import org.springframework.data.annotation.Id;
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

    private String email;
    private String otp;
    private LocalDateTime expiryTime;
}
