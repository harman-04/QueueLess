package com.queueless.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

@Data
@Schema(description = "Additional details for a token")
public class TokenRequestDTO {

    @NotBlank(message = "Purpose is required")
    @Schema(description = "Purpose of joining", example = "Consultation", requiredMode = Schema.RequiredMode.REQUIRED)
    private String purpose;

    @NotNull(message = "Condition is required")
    @Schema(description = "Medical condition or special note", example = "Fever" , requiredMode = Schema.RequiredMode.REQUIRED)
    private String condition;

    @Schema(description = "Additional notes", example = "Bring previous reports")
    private String notes;

    @Schema(description = "Custom key‑value fields")
    private Map<String, String> customFields;

    @Schema(description = "Whether details are private", example = "false", defaultValue = "false")
    private Boolean isPrivate;

    @Schema(description = "Whether details are visible to provider", example = "true", defaultValue = "true")
    private Boolean visibleToProvider;

    @Schema(description = "Whether details are visible to admin", example = "true", defaultValue = "true")
    private Boolean visibleToAdmin;
}