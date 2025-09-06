package com.queueless.backend.repository;

import com.queueless.backend.model.Queue;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface QueueRepository extends MongoRepository<Queue, String> {
    List<Queue> findByProviderId(String providerId);
    List<Queue> findByPlaceId(String placeId);
    List<Queue> findByServiceId(String serviceId);
    List<Queue> findByPlaceIdAndServiceId(String placeId, String serviceId);
    Queue findByProviderIdAndId(String providerId, String id);
    List<Queue> findByIsActive(boolean isActive);
}