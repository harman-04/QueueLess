package com.queueless.backend.repository;

import com.queueless.backend.model.Place;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PlaceRepository extends MongoRepository<Place, String> {
    List<Place> findByAdminId(String adminId);
    List<Place> findByType(String type);
    List<Place> findByIsActive(Boolean isActive);

    @Query("{ 'location' : { $near : { $geometry : { type : 'Point', coordinates: [?0, ?1] }, $maxDistance: ?2 } } }")
    List<Place> findByLocationNear(Double longitude, Double latitude, Double maxDistance);

    List<Place> findByNameContainingIgnoreCase(String name);
}