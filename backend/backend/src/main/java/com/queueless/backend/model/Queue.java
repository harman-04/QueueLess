package com.queueless.backend.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Document(collection = "queues")
@CompoundIndexes({
        @CompoundIndex(name = "provider_id_index", def = "{'providerId': 1}"),
        @CompoundIndex(name = "place_id_index", def = "{'placeId': 1}"),
        @CompoundIndex(name = "service_id_index", def = "{'serviceId': 1}"),
        @CompoundIndex(name = "active_status_index", def = "{'isActive': 1}")
})
@Data
@NoArgsConstructor
public class Queue {
    @Id
    private String id;

    @Field("placeId")
    private String placeId;

    @Field("serviceId")
    private String serviceId;

    @Field("providerId")
    private String providerId;

    @Field("serviceName")
    private String serviceName;

    @Field("maxCapacity")
    private Integer maxCapacity;

    @Field("currentPosition")
    private Integer currentPosition = 0;

    @Field("estimatedWaitTime")
    private Integer estimatedWaitTime = 0;

    @Field("isActive")
    private Boolean isActive = true;

    @Field("startTime")
    private LocalDateTime startTime;

    @Field("endTime")
    private LocalDateTime endTime;

    @Field("tokens")
    private List<QueueToken> tokens = new ArrayList<>();

    @Field("pendingEmergencyTokens")
    private List<QueueToken> pendingEmergencyTokens = new ArrayList<>();

    @Field("tokenCounter")
    private Integer tokenCounter = 0;

    @Field("statistics")
    private QueueStatistics statistics;

    @Field("supportsGroupToken")
    private Boolean supportsGroupToken = false;

    @Field("emergencySupport")
    private Boolean emergencySupport = false;

    @Field("emergencyPriorityWeight")
    private Integer emergencyPriorityWeight = 10;

    @Field("requiresEmergencyApproval")
    private Boolean requiresEmergencyApproval = false;

    @Field("autoApproveEmergency")
    private Boolean autoApproveEmergency = false;

    public Queue(String providerId, String serviceName, String placeId, String serviceId) {
        this.providerId = providerId;
        this.serviceName = serviceName;
        this.placeId = placeId;
        this.serviceId = serviceId;
        this.tokenCounter = 0;
        this.tokens = new ArrayList<>();
        this.pendingEmergencyTokens = new ArrayList<>();
        this.isActive = true;
        this.startTime = LocalDateTime.now();
        this.statistics = new QueueStatistics();
    }

    @Data
    @NoArgsConstructor
    public static class QueueStatistics {
        @Field("averageWaitTime")
        private Double averageWaitTime = 0.0;

        @Field("dailyUsersServed")
        private Integer dailyUsersServed = 0;

        @Field("busiestHours")
        private Map<String, Integer> busiestHours;

        @Field("totalServed")
        private Integer totalServed = 0;

        @Field("totalCancelled")
        private Integer totalCancelled = 0;

        @Field("averageRating")
        private Double averageRating = 0.0;

        @Field("dailyAverages")
        private Map<String, Integer> dailyAverages;

        @Field("maxDailyCapacity")
        private Integer maxDailyCapacity = 0;
    }
}