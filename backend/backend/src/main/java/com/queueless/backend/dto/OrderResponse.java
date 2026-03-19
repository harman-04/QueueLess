package com.queueless.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "Response after creating a Razorpay order")
public class OrderResponse {
    @Schema(description = "Razorpay order ID", example = "order_IEIzJzqZqZqZqZ")
    private String orderId;

    @Schema(description = "Amount in paise", example = "10000")
    private int amount;

    @Schema(description = "Currency code", example = "INR")
    private String currency;
}