package com.queueless.backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "services")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Service {
    @Id
    private String id;

    @Field("placeId")
    private String placeId;

    @Field("name")
    private String name;

    @Field("description")
    private String description;

    @Field("averageServiceTime")
    private Integer averageServiceTime; // in minutes

    @Field("supportsGroupToken")
    private Boolean supportsGroupToken = false;

    @Field("emergencySupport")
    private Boolean emergencySupport = false;

    @Field("isActive")
    private Boolean isActive = true;
}