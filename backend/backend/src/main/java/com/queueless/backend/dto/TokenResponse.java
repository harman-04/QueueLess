package com.queueless.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "Response containing a generated token value")
public class TokenResponse {
    @Schema(description = "The generated token (e.g., ADMIN-abc123)", example = "ADMIN-abc123")
    private String tokenValue;
}