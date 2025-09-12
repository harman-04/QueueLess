// Feedback.java
package com.queueless.backend.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Document(collection = "feedbacks")
@Data
@NoArgsConstructor
public class Feedback {
    @Id
    private String id;

    @Field("queueId")
    private String queueId;

    @Field("tokenId")
    private String tokenId;

    @Field("userId")
    private String userId;

    @Field("providerId")
    private String providerId;

    @Field("placeId")
    private String placeId;

    @Field("serviceId")
    private String serviceId;

    @Field("rating")
    private Integer rating; // 1-5 scale

    @Field("comment")
    private String comment;

    @Field("staffRating")
    private Integer staffRating; // 1-5 scale

    @Field("serviceRating")
    private Integer serviceRating; // 1-5 scale

    @Field("waitTimeRating")
    private Integer waitTimeRating; // 1-5 scale

    @Field("createdAt")
    private LocalDateTime createdAt;

    public Feedback(String queueId, String tokenId, String userId, String providerId,
                    String placeId, String serviceId) {
        this.queueId = queueId;
        this.tokenId = tokenId;
        this.userId = userId;
        this.providerId = providerId;
        this.placeId = placeId;
        this.serviceId = serviceId;
        this.createdAt = LocalDateTime.now();
    }
}