package com.queueless.backend.repository;

import com.queueless.backend.model.Service;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ServiceRepository extends MongoRepository<Service, String> {
    List<Service> findByPlaceId(String placeId);
    List<Service> findByPlaceIdAndIsActive(String placeId, Boolean isActive);
    List<Service> findByNameContainingIgnoreCase(String name);
}