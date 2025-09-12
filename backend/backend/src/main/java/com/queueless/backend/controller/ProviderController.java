package com.queueless.backend.controller;

import com.queueless.backend.dto.PlaceDTO;
import com.queueless.backend.dto.ServiceDTO;
import com.queueless.backend.model.User;
import com.queueless.backend.repository.UserRepository;
import com.queueless.backend.service.PlaceService;
import com.queueless.backend.service.ServiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/providers")
@RequiredArgsConstructor
public class ProviderController {

    private final UserRepository userRepository;
    private final PlaceService placeService;
    private final ServiceService serviceService;

    @GetMapping("/my-places")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<List<PlaceDTO>> getMyManagedPlaces(Authentication authentication) {
        String providerId = authentication.getName();
        User provider = userRepository.findById(providerId)
                .orElseThrow(() -> new RuntimeException("Provider not found"));

        List<PlaceDTO> places;
        if (provider.getManagedPlaceIds() != null && !provider.getManagedPlaceIds().isEmpty()) {
            places = placeService.getPlacesByIds(provider.getManagedPlaceIds()).stream()
                    .map(PlaceDTO::fromEntity)
                    .collect(Collectors.toList());
        } else {
            places = placeService.getPlacesByAdminId(provider.getAdminId()).stream()
                    .map(PlaceDTO::fromEntity)
                    .collect(Collectors.toList());
        }

        return ResponseEntity.ok(places);
    }

    @GetMapping("/my-services")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<List<ServiceDTO>> getMyManagedServices(Authentication authentication) {
        String providerId = authentication.getName();
        User provider = userRepository.findById(providerId)
                .orElseThrow(() -> new RuntimeException("Provider not found"));

        List<String> placeIds;
        if (provider.getManagedPlaceIds() != null && !provider.getManagedPlaceIds().isEmpty()) {
            placeIds = provider.getManagedPlaceIds();
        } else {
            placeIds = placeService.getPlacesByAdminId(provider.getAdminId())
                    .stream().map(place -> place.getId()).collect(Collectors.toList());
        }

        List<ServiceDTO> services = placeIds.stream()
                .flatMap(placeId -> serviceService.getServicesByPlaceId(placeId).stream())
                .map(ServiceDTO::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(services);
    }
}