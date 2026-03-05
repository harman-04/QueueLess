package com.queueless.backend.controller;

import com.queueless.backend.dto.ServiceDTO;
import com.queueless.backend.model.Service;
import com.queueless.backend.service.PlaceService;
import com.queueless.backend.service.ServiceService;
import com.queueless.backend.security.annotations.AdminOnly;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
@Tag(name = "Services", description = "Endpoints for managing services")
public class ServiceController {

    private final ServiceService serviceService;
    private final PlaceService placeService;

    @PostMapping
    @AdminOnly
    @Operation(summary = "Create a new service", description = "Creates a new service under a specific place. Only admin of the place can create.")
    @ApiResponse(responseCode = "200", description = "Service created",
            content = @Content(schema = @Schema(implementation = ServiceDTO.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden – not the admin of the place")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public ResponseEntity<ServiceDTO> createService(@Valid @RequestBody ServiceDTO serviceDTO) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String adminId = authentication.getName(); // Principal is the user ID

        if (authentication == null || adminId == null) {
            log.error("Authentication is null for createService");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("Request to create service: {} by admin: {}", serviceDTO, adminId);

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
    @Operation(summary = "Get service by ID", description = "Returns a single service by its ID. Public access.")
    @ApiResponse(responseCode = "200", description = "Service found",
            content = @Content(schema = @Schema(implementation = ServiceDTO.class)))
    @ApiResponse(responseCode = "404", description = "Service not found")
    public ResponseEntity<ServiceDTO> getService(@PathVariable String id) {
        log.debug("Fetching service with ID: {}", id);
        try {
            Service service = serviceService.getServiceById(id);
            log.info("Fetched service: {}", service);
            return ResponseEntity.ok(ServiceDTO.fromEntity(service));
        } catch (Exception e) {
            log.error("Error fetching service: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/place/{placeId}")
    @Operation(summary = "Get services by place ID", description = "Returns all services for a given place. Public access.")
    @ApiResponse(responseCode = "200", description = "List of services")
    public ResponseEntity<List<ServiceDTO>> getServicesByPlace(@PathVariable String placeId) {
        log.debug("Fetching services for place ID: {}", placeId);
        List<Service> services = serviceService.getServicesByPlaceId(placeId);
        log.info("Fetched {} services for place {}", services.size(), placeId);
        return ResponseEntity.ok(services.stream().map(ServiceDTO::fromEntity).collect(Collectors.toList()));
    }

    @PutMapping("/{id}")
    @AdminOnly
    @Operation(summary = "Update a service", description = "Updates an existing service. Only admin of the owning place can update.")
    @ApiResponse(responseCode = "200", description = "Service updated",
            content = @Content(schema = @Schema(implementation = ServiceDTO.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden – not the admin")
    @ApiResponse(responseCode = "404", description = "Service not found")
    public ResponseEntity<ServiceDTO> updateService(@PathVariable String id, @Valid @RequestBody ServiceDTO serviceDTO) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            log.error("Authentication is null for updateService");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String adminId = authentication.getName();
        log.info("Updating service with ID: {} | Data: {} by admin: {}", id, serviceDTO, adminId);

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
    @AdminOnly
    @Operation(summary = "Delete a service", description = "Deletes an existing service. Only admin of the owning place can delete.")
    @ApiResponse(responseCode = "200", description = "Service deleted")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden – not the admin")
    @ApiResponse(responseCode = "404", description = "Service not found")
    public ResponseEntity<Void> deleteService(@PathVariable String id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            log.error("Authentication is null for deleteService");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String adminId = authentication.getName();
        log.warn("Deleting service with ID: {} by admin: {}", id, adminId);

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

    @GetMapping
    @Operation(summary = "Get all services", description = "Returns all services. Public access. Consider adding pagination if needed.")
    @ApiResponse(responseCode = "200", description = "List of all services")
    public ResponseEntity<List<ServiceDTO>> getAllServices() {
        log.debug("Fetching all services");
        List<Service> services = serviceService.getAllServices();
        log.info("Fetched {} services", services.size());
        return ResponseEntity.ok(services.stream().map(ServiceDTO::fromEntity).collect(Collectors.toList()));
    }
}