package com.queueless.backend.dto;

import com.queueless.backend.model.Place;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "Place with current queue statistics")
public class PlaceWithQueueDTO {
    @Schema(description = "Place ID", example = "67b1a2c3d4e5f67890123456")
    private String id;

    @Schema(description = "Place name", example = "Central Hospital")
    private String name;

    @Schema(description = "Place type", example = "HOSPITAL")
    private String type;

    @Schema(description = "Place address", example = "123 Main St, City")
    private String address;

    @Schema(description = "Location coordinates [longitude, latitude]", example = "[72.8777, 19.0760]")
    private double[] location;

    @Schema(description = "Number of waiting tokens", example = "5")
    private int waitingTokens;

    @Schema(description = "Number of tokens in service", example = "2")
    private int inServiceTokens;

    @Schema(description = "Total active tokens (waiting + in‑service)", example = "7")
    private int totalActiveTokens;

    @Schema(description = "Admin ID who owns this place", example = "67b1a2c3d4e5f67890123455")
    private String adminId;

    public PlaceWithQueueDTO(Place place, int waitingTokens, int inServiceTokens) {
        this.id = place.getId();
        this.name = place.getName();
        this.type = place.getType();
        this.address = place.getAddress();
        if (place.getLocation() != null) {
            this.location = new double[]{place.getLocation().getX(), place.getLocation().getY()};
        }
        this.waitingTokens = waitingTokens;
        this.inServiceTokens = inServiceTokens;
        this.totalActiveTokens = waitingTokens + inServiceTokens;
        this.adminId = place.getAdminId();
    }
}