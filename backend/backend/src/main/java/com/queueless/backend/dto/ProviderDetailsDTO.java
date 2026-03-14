package com.queueless.backend.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderDetailsDTO {
    private String id;
    private String name;
    private String email;
    private String phoneNumber;
    private String profileImageUrl;
    private Boolean isVerified;
    private Boolean isActive;
    private String adminId;
    private List<String> managedPlaceIds;
    private List<PlaceDTO> managedPlaces; // detailed places
    private int totalQueues;
    private int activeQueues;
    private long tokensServedToday;
    private long tokensServedTotal;
    private double averageRating;
    private double cancellationRate;
}