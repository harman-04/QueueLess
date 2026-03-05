package com.queueless.backend.dto;

import com.queueless.backend.model.Place;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PlaceWithQueueDTO {
    private String id;
    private String name;
    private String type;
    private String address;
    private double[] location; // [longitude, latitude]
    private int waitingTokens;
    private int inServiceTokens;
    private int totalActiveTokens;
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