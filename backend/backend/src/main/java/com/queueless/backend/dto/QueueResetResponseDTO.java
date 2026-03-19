package com.queueless.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Response after resetting a queue")
public class QueueResetResponseDTO {
    @Schema(description = "Whether the reset was successful", example = "true")
    private Boolean success;

    @Schema(description = "Status message", example = "Queue reset successfully")
    private String message;

    @Schema(description = "URL to download the exported file (if preserveData was true)", example = "/export/exports/export-123456789")
    private String exportFileUrl;

    @Schema(description = "Number of tokens that were reset", example = "25")
    private Integer tokensReset;
}