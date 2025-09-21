package com.queueless.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueueToken {
    @Field("tokenId")
    private String tokenId;

    @Field("userId")
    private String userId;

    @Field("userName")
    private String userName; // Store name for quick access

    @Field("userDetails")
    private UserQueueDetails userDetails; // Add this field

    @Field("status")
    private String status;

    @Field("issuedAt")
    private LocalDateTime issuedAt;

    @Field("isGroup")
    private Boolean isGroup = false;

    @Field("groupMembers")
    private List<GroupMember> groupMembers;

    @Field("groupSize")
    private Integer groupSize = 1;

    @Field("isEmergency")
    private Boolean isEmergency = false;

    @Field("emergencyDetails")
    private String emergencyDetails;

    @Field("priority")
    private Integer priority = 0;

    @Field("servedAt")
    private LocalDateTime servedAt;

    @Field("completedAt")
    private LocalDateTime completedAt;


    // Add this field
    @Field("serviceDurationMinutes")
    private Long serviceDurationMinutes;



    // Constructor for regular tokens
    public QueueToken(String tokenId, String userId, String status, LocalDateTime issuedAt) {
        this.tokenId = tokenId;
        this.userId = userId;
        this.status = status;
        this.issuedAt = issuedAt;
        this.isGroup = false;
        this.isEmergency = false;
        this.priority = 0;
    }

    // Constructor for regular tokens with userName
    public QueueToken(String tokenId, String userId, String userName, String status, LocalDateTime issuedAt) {
        this.tokenId = tokenId;
        this.userId = userId;
        this.userName = userName;
        this.status = status;
        this.issuedAt = issuedAt;
        this.isGroup = false;
        this.isEmergency = false;
        this.priority = 0;
        this.serviceDurationMinutes = null; // Initialize as null
    }


    // Constructor for group tokens
    public QueueToken(String tokenId, String userId, String userName, String status, LocalDateTime issuedAt,
                      List<GroupMember> groupMembers, Integer groupSize) {
        this.tokenId = tokenId;
        this.userId = userId;
        this.userName = userName;
        this.status = status;
        this.issuedAt = issuedAt;
        this.isGroup = true;
        this.groupMembers = groupMembers;
        this.groupSize = groupSize;
        this.isEmergency = false;
        this.priority = 0;
    }

    // Constructor for emergency tokens
    public QueueToken(String tokenId, String userId, String userName, String status, LocalDateTime issuedAt,
                      String emergencyDetails, Integer priority) {
        this.tokenId = tokenId;
        this.userId = userId;
        this.userName = userName;
        this.status = status;
        this.issuedAt = issuedAt;
        this.isGroup = false;
        this.isEmergency = true;
        this.emergencyDetails = emergencyDetails;
        this.priority = priority;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GroupMember {
        @Field("name")
        private String name;

        @Field("details")
        private String details;
    }
}