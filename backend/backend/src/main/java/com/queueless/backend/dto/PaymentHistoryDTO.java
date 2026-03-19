package com.queueless.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payment history entry")
public class PaymentHistoryDTO {
    @Schema(description = "Payment or token ID", example = "pay123")
    private String id;

    @Schema(description = "Description", example = "Admin token")
    private String description;

    @Schema(description = "Amount in paise", example = "10000")
    private int amount;

    @Schema(description = "Role (ADMIN or PROVIDER)", example = "ADMIN")
    private String role;

    @Schema(description = "Payment status", example = "Completed")
    private String status;

    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Reference (orderId or token value)", example = "order_abc123")
    private String reference;

    @Schema(description = "Whether payment was completed", example = "true")
    private boolean isPaid;
}