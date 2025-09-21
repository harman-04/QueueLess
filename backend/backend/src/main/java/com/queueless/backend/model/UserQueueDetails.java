package com.queueless.backend.model;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Map;

@Data
public class UserQueueDetails {
    @Field("purpose")
    private String purpose;

    @Field("condition")
    private String condition; // For medical queues

    @Field("notes")
    private String notes;

    @Field("customFields")
    private Map<String, String> customFields;

    @Field("isPrivate")
    private Boolean isPrivate = false;

    @Field("visibleToProvider")
    private Boolean visibleToProvider = true;

    @Field("visibleToAdmin")
    private Boolean visibleToAdmin = true;
}