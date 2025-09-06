package com.queueless.backend.service;

import com.queueless.backend.dto.ServiceDTO;
import com.queueless.backend.model.Service;
import com.queueless.backend.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


import java.util.List;
import java.util.Optional;

@Slf4j
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ServiceService {

    private final ServiceRepository serviceRepository;
    private final PlaceService placeService;


    public Service createService(ServiceDTO serviceDTO) {
        log.debug("Creating new service: {}", serviceDTO);

        // Verify the place exists and belongs to the admin
        // This would need to be implemented based on your authentication context

        Service service = new Service();
        service.setPlaceId(serviceDTO.getPlaceId());
        service.setName(serviceDTO.getName());
        service.setDescription(serviceDTO.getDescription());
        service.setAverageServiceTime(serviceDTO.getAverageServiceTime());
        service.setSupportsGroupToken(serviceDTO.getSupportsGroupToken());
        service.setEmergencySupport(serviceDTO.getEmergencySupport());
        service.setIsActive(serviceDTO.getIsActive());

        Service saved = serviceRepository.save(service);
        log.info("Service saved with ID: {}", saved.getId());
        return saved;
    }


    // Add this method to verify service ownership through place
    public boolean isServiceOwnedByAdmin(String serviceId, String adminId) {
        Optional<Service> service = serviceRepository.findById(serviceId);
        if (service.isPresent()) {
            String placeId = service.get().getPlaceId();
            return placeService.isPlaceOwnedByAdmin(placeId, adminId);
        }
        return false;
    }

    public Service getServiceById(String id) {
        log.debug("Looking up service with ID: {}", id);
        return serviceRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Service not found with ID: {}", id);
                    return new RuntimeException("Service not found with id: " + id);
                });
    }

    public List<Service> getServicesByPlaceId(String placeId) {
        log.debug("Fetching services by place ID: {}", placeId);
        List<Service> services = serviceRepository.findByPlaceId(placeId);
        log.info("Found {} services for place {}", services.size(), placeId);
        return services;
    }

    public Service updateService(String id, ServiceDTO serviceDTO) {
        log.info("Updating service with ID: {}", id);
        Service service = getServiceById(id);

        if (serviceDTO.getName() != null) service.setName(serviceDTO.getName());
        if (serviceDTO.getDescription() != null) service.setDescription(serviceDTO.getDescription());
        if (serviceDTO.getAverageServiceTime() != null) service.setAverageServiceTime(serviceDTO.getAverageServiceTime());
        if (serviceDTO.getSupportsGroupToken() != null) service.setSupportsGroupToken(serviceDTO.getSupportsGroupToken());
        if (serviceDTO.getEmergencySupport() != null) service.setEmergencySupport(serviceDTO.getEmergencySupport());
        if (serviceDTO.getIsActive() != null) service.setIsActive(serviceDTO.getIsActive());

        Service updated = serviceRepository.save(service);
        log.info("Service updated successfully with ID: {}", id);
        return updated;
    }

    public void deleteService(String id) {
        log.warn("Deleting service with ID: {}", id);
        Service service = getServiceById(id);
        serviceRepository.delete(service);
        log.info("Service deleted successfully with ID: {}", id);
    }

    // ServiceService.java - Add this method
    public List<Service> getAllServices() {
        log.debug("Fetching all services");
        List<Service> services = serviceRepository.findAll();
        log.info("Found {} services", services.size());
        return services;
    }
}
