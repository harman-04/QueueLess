// Update PlaceDTO to include all fields
package com.queueless.backend.dto;

import com.queueless.backend.model.Place;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class PlaceDTO {
    private String id;
    private String name;
    private String type;
    private String address;
    private double[] location; // [longitude, latitude]
    private String adminId;
    private List<String> imageUrls;
    private String description;
    private Double rating;
    private Integer totalRatings;
    private Map<String, String> contactInfo;
    private List<Place.BusinessHours> businessHours;
    private Boolean isActive;

    public static PlaceDTO fromEntity(Place place) {
        PlaceDTO dto = new PlaceDTO();
        dto.setId(place.getId());
        dto.setName(place.getName());
        dto.setType(place.getType());
        dto.setAddress(place.getAddress());
        // Convert GeoJsonPoint to double[]
        if (place.getLocation() != null) {
            dto.setLocation(new double[]{
                    place.getLocation().getX(), // longitude
                    place.getLocation().getY()  // latitude
            });
        }
        dto.setAdminId(place.getAdminId());
        dto.setImageUrls(place.getImageUrls());
        dto.setDescription(place.getDescription());
        dto.setRating(place.getRating());
        dto.setTotalRatings(place.getTotalRatings());
        dto.setContactInfo(place.getContactInfo());
        dto.setBusinessHours(place.getBusinessHours());
        dto.setIsActive(place.getIsActive());
        return dto;
    }
}