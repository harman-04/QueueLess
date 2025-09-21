package com.queueless.backend.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

@Data
public class TokenRequestDTO {
    @NotNull(message = "Purpose is required")
    private String purpose;

    @NotNull(message = "Condition is required")
    private String condition;

    private String notes;
    private Map<String, String> customFields;
    private Boolean isPrivate;
    private Boolean visibleToProvider;
    private Boolean visibleToAdmin;
}