package com.queueless.backend.repository;

import com.queueless.backend.model.Service;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface ServiceRepository extends MongoRepository<Service, String> {
    List<Service> findByPlaceId(String placeId);
}