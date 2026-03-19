package com.queueless.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Search filters")
public class SearchRequestDTO {
    @Schema(description = "Search query", example = "hospital")
    private String query;

    @Schema(description = "Place type (single)", example = "HOSPITAL")
    private String placeType;

    @Schema(description = "List of place types", example = "[\"HOSPITAL\",\"CLINIC\"]")
    private List<String> placeTypes = new ArrayList<>();

    @Schema(description = "List of place IDs to filter")
    private List<String> placeIds = new ArrayList<>();

    @Min(0) @Max(5)
    @Schema(description = "Minimum rating (0‑5)", example = "4.0", defaultValue = "0.0")
    private Double minRating = 0.0;

    @Positive
    @Schema(description = "Maximum wait time in minutes", example = "15")
    private Integer maxWaitTime;

    @Schema(description = "Filter queues that support group tokens", example = "true")
    private Boolean supportsGroupToken;

    @Schema(description = "Filter queues that support emergency tokens", example = "true")
    private Boolean emergencySupport;

    @Schema(description = "Include only active places/queues", example = "true", defaultValue = "true")
    private Boolean isActive = true;

    @Schema(description = "Longitude for location‑based search", example = "10.0")
    private Double longitude;

    @Schema(description = "Latitude for location‑based search", example = "20.0")
    private Double latitude;

    @Positive
    @Schema(description = "Search radius in km", example = "5.0", defaultValue = "5.0")
    private Double radius = 5.0;

    @Schema(description = "Whether to search in places", example = "true", defaultValue = "true")
    private Boolean searchPlaces = true;

    @Schema(description = "Whether to search in services", example = "true", defaultValue = "true")
    private Boolean searchServices = true;

    @Schema(description = "Whether to search in queues", example = "true", defaultValue = "true")
    private Boolean searchQueues = true;

    @Schema(description = "Sort field", example = "name", defaultValue = "name")
    private String sortBy = "name";

    @Schema(description = "Sort direction", example = "asc", defaultValue = "asc")
    private String sortDirection = "asc";

    // Add these getter methods
    public Boolean isSearchPlaces() {
        return searchPlaces != null ? searchPlaces : true;
    }

    public Boolean isSearchServices() {
        return searchServices != null ? searchServices : true;
    }

    public Boolean isSearchQueues() {
        return searchQueues != null ? searchQueues : true;
    }
}