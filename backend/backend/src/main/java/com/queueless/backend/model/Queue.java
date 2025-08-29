// src/main/java/com/queueless/backend/model/Queue.java
package com.queueless.backend.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.ArrayList;

@Document(collection = "queues")
@Data
@NoArgsConstructor
public class Queue {

    @Id
    private String id;
    private String providerId;
    private String serviceName;
    private boolean isActive = true; // Changed from 'active' to 'isActive'
    private List<QueueToken> tokens = new ArrayList<>();
    private int tokenCounter;

    public Queue(String providerId, String serviceName) {
        this.providerId = providerId;
        this.serviceName = serviceName;
        this.tokenCounter = 0;
        this.tokens = new ArrayList<>();
        this.isActive = true;
    }
}