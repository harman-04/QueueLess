package com.queueless.backend.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.util.List;
import java.util.ArrayList;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequestDTO {
    private String query;
    private String placeType;
    private List<String> placeTypes = new ArrayList<>();
    private List<String> placeIds = new ArrayList<>();

    @Min(0) @Max(5)
    private Double minRating = 0.0;

    private Integer maxWaitTime;
    private Boolean supportsGroupToken;
    private Boolean emergencySupport;
    private Boolean isActive = true;

    private Double longitude;
    private Double latitude;
    private Double radius = 5.0; // in kilometers

    private Boolean searchPlaces = true;
    private Boolean searchServices = true;
    private Boolean searchQueues = true;

    private String sortBy = "name";
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