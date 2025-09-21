package com.queueless.backend.dto;

import com.queueless.backend.model.Place;
import com.queueless.backend.model.Place.BusinessHours;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlaceDTO {
    private String id;

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Type is required")
    private String type;

    @NotBlank(message = "Address is required")
    private String address;

    @NotNull(message = "Location is required")
    @Size(min = 2, max = 2, message = "Location must contain longitude and latitude")
    private double[] location; // [longitude, latitude]

    @NotBlank(message = "Admin ID is required")
    private String adminId;
    private List<String> imageUrls;
    private String description;
    private Double rating;
    private Integer totalRatings;
    private Map<String, String> contactInfo;
    private List<BusinessHours> businessHours;

    @NotNull(message = "Is active field is required")
    private Boolean isActive;

    public static PlaceDTO fromEntity(Place place) {
        PlaceDTO dto = new PlaceDTO();
        dto.setId(place.getId());
        dto.setName(place.getName());
        dto.setType(place.getType());
        dto.setAddress(place.getAddress());
        if (place.getLocation() != null) {
            dto.setLocation(new double[]{
                    place.getLocation().getX(),
                    place.getLocation().getY()
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