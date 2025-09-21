package com.queueless.backend.repository;

import com.queueless.backend.model.Queue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
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
    List<Queue> findByPlaceIdIn(List<String> placeIds);


    // Search queues with multiple filters
    @Query("{"
            + "'$and': ["
            + "  { 'serviceName': { '$regex': ?0, '$options': 'i' } },"
            + "  { 'placeId': { '$in': ?1 } },"
            + "  { 'isActive': ?2 },"
            + "  { 'estimatedWaitTime': { '$lte': ?3 } },"
            + "  { 'supportsGroupToken': ?4 },"
            + "  { 'emergencySupport': ?5 }"
            + "]"
            + "}")
    Page<Queue> advancedSearch(String serviceName, List<String> placeIds, Boolean isActive,
                               Integer maxWaitTime, Boolean supportsGroupToken,
                               Boolean emergencySupport, Pageable pageable);

    // Find queues by wait time range
    @Query("{"
            + "'estimatedWaitTime': { '$gte': ?0, '$lte': ?1 },"
            + "'isActive': true"
            + "}")
    List<Queue> findByWaitTimeRange(Integer minWaitTime, Integer maxWaitTime);

    // Count waiting tokens by queue
    @Query(value = "{ '_id': ?0 }", fields = "{ 'tokens': 1 }")
    Queue findTokensByQueueId(String queueId);

}