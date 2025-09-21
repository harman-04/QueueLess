package com.queueless.backend.dto;

import com.queueless.backend.model.Service;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceDTO {
    private String id;

    @NotBlank(message = "Place ID is required")
    private String placeId;

    @NotBlank(message = "Service name is required")
    private String name;

    private String description;

    @Min(value = 1, message = "Average service time must be at least 1 minute")
    private Integer averageServiceTime;

    @NotNull(message = "Supports group token field is required")
    private Boolean supportsGroupToken;

    @NotNull(message = "Emergency support field is required")
    private Boolean emergencySupport;

    @NotNull(message = "Is active field is required")
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