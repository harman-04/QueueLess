package com.queueless.backend.model;

import com.queueless.backend.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document("payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    @Id
    private String id;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private int amount;
    private boolean isPaid;
    private String createdForEmail;
    private Role role;
    private LocalDateTime createdAt;
    private String createdByAdminId; // For provider tokens: which admin purchased this token
}