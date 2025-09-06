// Update ServiceDTO to include all fields
package com.queueless.backend.dto;

import com.queueless.backend.model.Service;
import lombok.Data;

@Data
public class ServiceDTO {
    private String id;
    private String placeId;
    private String name;
    private String description;
    private Integer averageServiceTime;
    private Boolean supportsGroupToken;
    private Boolean emergencySupport;
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