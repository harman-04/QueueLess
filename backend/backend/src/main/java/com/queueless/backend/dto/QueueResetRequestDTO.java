package com.queueless.backend.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Data
public class QueueResetRequestDTO {
    @NotNull(message = "Preserve data flag is required")
    private Boolean preserveData;

    @Pattern(regexp = "^(tokens|statistics|full)$", message = "Report type must be 'tokens', 'statistics', or 'full'")
    private String reportType;

    @NotNull(message = "Include user details flag is required")
    private Boolean includeUserDetails;
}