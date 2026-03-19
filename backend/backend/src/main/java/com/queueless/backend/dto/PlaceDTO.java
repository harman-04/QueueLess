package com.queueless.backend.dto;

import com.queueless.backend.model.Place;
import com.queueless.backend.model.Place.BusinessHours;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Place information")
public class PlaceDTO {
    @Schema(description = "Place ID", example = "507f1f77bcf86cd799439011")
    private String id;

    @NotBlank(message = "Name is required")
    @Schema(description = "Place name", example = "Central Hospital", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotBlank(message = "Type is required")
    @Schema(description = "Place type", example = "HOSPITAL", requiredMode = Schema.RequiredMode.REQUIRED)
    private String type;

    @NotBlank(message = "Address is required")
    @Schema(description = "Street address", example = "123 Main St, Springfield", requiredMode = Schema.RequiredMode.REQUIRED)
    private String address;

    @NotNull(message = "Location is required")
    @Size(min = 2, max = 2, message = "Location must contain longitude and latitude")
    @Schema(description = "Geographic coordinates [longitude, latitude]", example = "[10.0, 20.0]", requiredMode = Schema.RequiredMode.REQUIRED)
    private double[] location;

    @NotBlank(message = "Admin ID is required")
    @Schema(description = "ID of the admin who owns this place", example = "507f1f77bcf86cd799439012", requiredMode = Schema.RequiredMode.REQUIRED)
    private String adminId;

    @Schema(description = "List of image URLs")
    private List<String> imageUrls;

    @Schema(description = "Place description", example = "A 24/7 emergency hospital")
    private String description;

    @Schema(description = "Average rating", example = "4.5")
    private Double rating;

    @Schema(description = "Total number of ratings", example = "120")
    private Integer totalRatings;

    @Schema(description = "Contact information (phone, email, website)")
    private Map<String, String> contactInfo;

    @Valid
    @Schema(description = "Business hours")
    private List<BusinessHours> businessHours;

    @NotNull(message = "Is active field is required")
    @Schema(description = "Whether the place is currently active", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
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