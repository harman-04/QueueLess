// src/main/java/com/queueless/backend/dto/PaymentHistoryDTO.java
package com.queueless.backend.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentHistoryDTO {
    private String id;               // paymentId or tokenId
    private String description;       // e.g., "Admin token" or "Provider token for p@gmail.com"
    private int amount;               // amount in paise (0 for tokens without payment)
    private String role;              // ADMIN or PROVIDER
    private String status;            // "Completed" or "Pending" (based on payment, always "Completed" for tokens)
    private LocalDateTime createdAt;
    private String reference;         // orderId or tokenValue
    private boolean isPaid;           // true for tokens
}