package com.queueless.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.Map;

@Data
@Schema(description = "User details for a token (privacy‑aware)")
public class UserDetailsResponseDTO {
    @Schema(description = "User ID", example = "67b1a2c3d4e5f67890123460")
    private String userId;

    @Schema(description = "User's name", example = "John Doe")
    private String userName;

    @Schema(description = "Purpose of joining (e.g., consultation)", example = "Fever")
    private String purpose;

    @Schema(description = "Medical condition or special notes", example = "High fever")
    private String condition;

    @Schema(description = "Additional notes", example = "Bring ID card")
    private String notes;

    @Schema(description = "Custom fields (key‑value pairs)")
    private Map<String, String> customFields;

    @Schema(description = "Whether details are visible to the current requester", example = "true")
    private Boolean detailsVisible;
}