package com.queueless.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Request containing a token ID for operations like complete token")
public class TokenRequest {

    @NotBlank(message = "Token ID is required")
    @Schema(description = "ID of the token to operate on", example = "queue123-T-001" , requiredMode = Schema.RequiredMode.REQUIRED)
    private String tokenId;
}