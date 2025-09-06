package com.queueless.backend.controller;

import com.queueless.backend.dto.ServiceDTO;
import com.queueless.backend.model.Service;
import com.queueless.backend.service.PlaceService;
import com.queueless.backend.service.ServiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
public class ServiceController {

    private final ServiceService serviceService;
    private final PlaceService placeService;
    private String getUserIdFromAuthentication(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof String) {
            return (String) authentication.getPrincipal();
        }
        return null;
    }
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServiceDTO> createService(@Valid @RequestBody ServiceDTO serviceDTO) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String adminId = getUserIdFromAuthentication(authentication);
        if (authentication == null || authentication.getName() == null) {
            log.error("Authentication is null for createService");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("Request to create service: {} by admin: {}", serviceDTO, adminId);

        // Verify the place belongs to the admin
        if (!placeService.isPlaceOwnedByAdmin(serviceDTO.getPlaceId(), adminId)) {
            log.warn("Unauthorized attempt to create service for place={} by {}", serviceDTO.getPlaceId(), adminId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            Service service = serviceService.createService(serviceDTO);
            log.info("Service created successfully with ID: {}", service.getId());
            return ResponseEntity.ok(ServiceDTO.fromEntity(service));
        } catch (Exception e) {
            log.error("Error creating service: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceDTO> getService(@PathVariable String id) {
        log.debug("Fetching service with ID: {}", id);
        Service service = serviceService.getServiceById(id);
        log.info("Fetched service: {}", service);
        return ResponseEntity.ok(ServiceDTO.fromEntity(service));
    }

    @GetMapping("/place/{placeId}")
    public ResponseEntity<List<ServiceDTO>> getServicesByPlace(@PathVariable String placeId) {
        log.debug("Fetching services for place ID: {}", placeId);
        List<Service> services = serviceService.getServicesByPlaceId(placeId);
        log.info("Fetched {} services for place {}", services.size(), placeId);
        return ResponseEntity.ok(services.stream().map(ServiceDTO::fromEntity).collect(Collectors.toList()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServiceDTO> updateService(@PathVariable String id, @Valid @RequestBody ServiceDTO serviceDTO) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            log.error("Authentication is null for updateService");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String adminId = authentication.getName();
        log.info("Updating service with ID: {} | Data: {} by admin: {}", id, serviceDTO, adminId);

        // Verify the service belongs to the admin
        if (!serviceService.isServiceOwnedByAdmin(id, adminId)) {
            log.warn("Unauthorized attempt to update service={} by {}", id, adminId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            Service service = serviceService.updateService(id, serviceDTO);
            log.info("Service updated successfully with ID: {}", id);
            return ResponseEntity.ok(ServiceDTO.fromEntity(service));
        } catch (Exception e) {
            log.error("Error updating service: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteService(@PathVariable String id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            log.error("Authentication is null for deleteService");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String adminId = authentication.getName();
        log.warn("Deleting service with ID: {} by admin: {}", id, adminId);

        // Verify the service belongs to the admin
        if (!serviceService.isServiceOwnedByAdmin(id, adminId)) {
            log.warn("Unauthorized attempt to delete service={} by {}", id, adminId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            serviceService.deleteService(id);
            log.info("Service deleted successfully with ID: {}", id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error deleting service: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    // ServiceController.java - Add this method
    @GetMapping
    public ResponseEntity<List<ServiceDTO>> getAllServices() {
        log.debug("Fetching all services");
        List<Service> services = serviceService.getAllServices();
        log.info("Fetched {} services", services.size());
        return ResponseEntity.ok(services.stream().map(ServiceDTO::fromEntity).collect(Collectors.toList()));
    }
}
