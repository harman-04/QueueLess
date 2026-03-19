package com.queueless.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Detailed provider information including statistics and managed places")
public class ProviderDetailsDTO {
    @Schema(description = "Provider ID", example = "67b1a2c3d4e5f67890123458")
    private String id;

    @Schema(description = "Provider name", example = "Dr. Smith")
    private String name;

    @Schema(description = "Provider email", example = "provider@example.com")
    private String email;

    @Schema(description = "Provider phone number", example = "+91 9876543210")
    private String phoneNumber;

    @Schema(description = "Profile image URL", example = "/uploads/provider123_abc.jpg")
    private String profileImageUrl;

    @Schema(description = "Whether the email is verified", example = "true")
    private Boolean isVerified;

    @Schema(description = "Whether the account is active", example = "true")
    private Boolean isActive;

    @Schema(description = "Admin ID who created this provider", example = "67b1a2c3d4e5f67890123455")
    private String adminId;

    @Schema(description = "List of place IDs managed by this provider")
    private List<String> managedPlaceIds;

    @Schema(description = "Detailed place objects managed by this provider")
    private List<PlaceDTO> managedPlaces;

    @Schema(description = "Total number of queues owned by this provider", example = "5")
    private int totalQueues;

    @Schema(description = "Number of active queues", example = "3")
    private int activeQueues;

    @Schema(description = "Tokens served today", example = "120")
    private long tokensServedToday;

    @Schema(description = "Total tokens served all time", example = "15420")
    private long tokensServedTotal;

    @Schema(description = "Average rating (1-5)", example = "4.2")
    private double averageRating;

    @Schema(description = "Cancellation rate (percentage)", example = "5.5")
    private double cancellationRate;
}