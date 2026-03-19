package com.queueless.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "Request to reset a queue with options")
public class QueueResetRequestDTO {
    @NotNull(message = "Preserve data flag is required")
    @Schema(description = "Whether to export data before reset", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean preserveData;

    @Pattern(regexp = "^(tokens|statistics|full)$", message = "Report type must be 'tokens', 'statistics', or 'full'")
    @Schema(description = "Type of report to export", example = "full", allowableValues = {"tokens", "statistics", "full"})
    private String reportType;

    @NotNull(message = "Include user details flag is required")
    @Schema(description = "Whether to include user details in export", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean includeUserDetails;
}