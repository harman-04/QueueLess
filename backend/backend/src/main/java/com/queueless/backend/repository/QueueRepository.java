package com.queueless.backend.repository;

import com.queueless.backend.model.Queue;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QueueRepository extends MongoRepository<Queue, String> {

    List<Queue> findByProviderId(String providerId);

    Queue findByProviderIdAndId(String providerId, String id);

    // NEW: Add a method to find all queues where isActive is true
    List<Queue> findByIsActive(boolean isActive);
}