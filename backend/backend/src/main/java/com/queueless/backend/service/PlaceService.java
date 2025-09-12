package com.queueless.backend.service;

import com.queueless.backend.dto.PlaceDTO;
import com.queueless.backend.model.Place;
import com.queueless.backend.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceService {

    private final PlaceRepository placeRepository;

    public Place createPlace(PlaceDTO placeDTO) {
        log.debug("Creating new place: {}", placeDTO);

        Place place = new Place();
        place.setName(placeDTO.getName());
        place.setType(placeDTO.getType());
        place.setAddress(placeDTO.getAddress());

        if (placeDTO.getLocation() != null && placeDTO.getLocation().length == 2) {
            double lon = placeDTO.getLocation()[0];
            double lat = placeDTO.getLocation()[1];
            GeoJsonPoint geoPoint = new GeoJsonPoint(lon, lat);
            log.debug("Converted DTO location {} to GeoJsonPoint {}", placeDTO.getLocation(), geoPoint);
            place.setLocation(geoPoint);
        } else {
            log.warn("PlaceDTO location is null or invalid: {}", placeDTO.getLocation());
        }

        place.setAdminId(placeDTO.getAdminId());
        place.setImageUrls(placeDTO.getImageUrls());
        place.setDescription(placeDTO.getDescription());
        place.setRating(placeDTO.getRating() != null ? placeDTO.getRating() : 0.0);
        place.setTotalRatings(placeDTO.getTotalRatings() != null ? placeDTO.getTotalRatings() : 0);
        place.setContactInfo(placeDTO.getContactInfo());
        place.setBusinessHours(placeDTO.getBusinessHours());
        place.setIsActive(placeDTO.getIsActive() != null ? placeDTO.getIsActive() : true);

        return placeRepository.save(place);
    }

    public Place getPlaceById(String id) {
        log.debug("Fetching place with ID: {}", id);
        return placeRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Place not found with ID: {}", id);
                    return new RuntimeException("Place not found with id: " + id);
                });
    }

    public List<Place> getPlacesByAdminId(String adminId) {
        log.debug("Fetching places by admin ID: {}", adminId);
        List<Place> places = placeRepository.findByAdminId(adminId);
        log.info("Found {} places for admin {}", places.size(), adminId);
        return places;
    }

    public boolean isPlaceOwnedByAdmin(String placeId, String adminId) {
        Optional<Place> place = placeRepository.findById(placeId);
        return place.isPresent() && place.get().getAdminId().equals(adminId);
    }

    public List<Place> getPlacesByType(String type) {
        log.debug("Fetching places by type: {}", type);
        List<Place> places = placeRepository.findByType(type);
        log.info("Found {} places of type {}", places.size(), type);
        return places;
    }

    public List<Place> getNearbyPlaces(double longitude, double latitude, double radius) {
        log.debug("Searching nearby places [lon={}, lat={}, radius={}km]", longitude, latitude, radius);
        double maxDistance = radius * 1000;
        List<Place> places = placeRepository.findByLocationNear(longitude, latitude, maxDistance);
        log.info("Found {} nearby places", places.size());
        return places;
    }

    public Place updatePlace(String id, PlaceDTO placeDTO) {
        log.info("Updating place with ID: {}", id);
        Place place = getPlaceById(id);

        if (placeDTO.getName() != null) place.setName(placeDTO.getName());
        if (placeDTO.getType() != null) place.setType(placeDTO.getType());
        if (placeDTO.getAddress() != null) place.setAddress(placeDTO.getAddress());
        if (placeDTO.getLocation() != null && placeDTO.getLocation().length == 2) {
            place.setLocation(new GeoJsonPoint(placeDTO.getLocation()[0], placeDTO.getLocation()[1]));
        }
        if (placeDTO.getImageUrls() != null) place.setImageUrls(placeDTO.getImageUrls());
        if (placeDTO.getDescription() != null) place.setDescription(placeDTO.getDescription());
        if (placeDTO.getRating() != null) place.setRating(placeDTO.getRating());
        if (placeDTO.getTotalRatings() != null) place.setTotalRatings(placeDTO.getTotalRatings());
        if (placeDTO.getContactInfo() != null) place.setContactInfo(placeDTO.getContactInfo());
        if (placeDTO.getBusinessHours() != null) place.setBusinessHours(placeDTO.getBusinessHours());
        if (placeDTO.getIsActive() != null) place.setIsActive(placeDTO.getIsActive());

        Place updated = placeRepository.save(place);
        log.info("Place updated successfully with ID: {}", id);
        return updated;
    }

    public void deletePlace(String id) {
        log.warn("Deleting place with ID: {}", id);
        Place place = getPlaceById(id);
        placeRepository.delete(place);
        log.info("Place deleted successfully with ID: {}", id);
    }

    public List<Place> getAllPlaces() {
        log.debug("Fetching all places");
        List<Place> places = placeRepository.findAll();
        log.info("Found {} places", places.size());
        return places;
    }

    public List<Place> getPlacesByIds(List<String> placeIds) {
        log.debug("Fetching places by IDs: {}", placeIds);
        return placeRepository.findAllById(placeIds);
    }
}