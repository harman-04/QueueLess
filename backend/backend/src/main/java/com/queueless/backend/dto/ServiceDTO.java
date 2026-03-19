package com.queueless.backend.dto;

import com.queueless.backend.model.Service;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Service information")
public class ServiceDTO {
    @Schema(description = "Service ID", example = "67b1a2c3d4e5f67890123459")
    private String id;

    @NotBlank(message = "Place ID is required")
    @Schema(description = "ID of the place this service belongs to", example = "67b1a2c3d4e5f67890123456" , requiredMode = Schema.RequiredMode.REQUIRED)
    private String placeId;

    @NotBlank(message = "Service name is required")
    @Schema(description = "Service name", example = "General Consultation" , requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "Service description", example = "General medical consultation")
    private String description;

    @NotNull(message = "Average service time is required")
    @Min(value = 1, message = "Average service time must be at least 1 minute")
    @Schema(description = "Average service time in minutes", example = "15" , requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer averageServiceTime;

    @NotNull(message = "Supports group token field is required")
    @Schema(description = "Whether group tokens are supported", example = "true" , requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean supportsGroupToken;

    @NotNull(message = "Emergency support field is required")
    @Schema(description = "Whether emergency tokens are supported", example = "false" ,requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean emergencySupport;

    @NotNull(message = "Is active field is required")
    @Schema(description = "Whether the service is active", example = "true" , requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean isActive;

    public static ServiceDTO fromEntity(Service service) {
        ServiceDTO dto = new ServiceDTO();
        dto.setId(service.getId());
        dto.setPlaceId(service.getPlaceId());
        dto.setName(service.getName());
        dto.setDescription(service.getDescription());
        dto.setAverageServiceTime(service.getAverageServiceTime());
        dto.setSupportsGroupToken(service.getSupportsGroupToken());
        dto.setEmergencySupport(service.getEmergencySupport());
        dto.setIsActive(service.getIsActive());
        return dto;
    }
}