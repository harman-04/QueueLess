package com.queueless.backend.repository;

import com.queueless.backend.model.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ServiceRepository extends MongoRepository<Service, String> {
    List<Service> findByPlaceId(String placeId);
    List<Service> findByPlaceIdAndIsActive(String placeId, Boolean isActive);
    List<Service> findByNameContainingIgnoreCase(String name);



    // Advanced service search
    @Query("{"
            + "'$and': ["
            + "  { 'name': { '$regex': ?0, '$options': 'i' } },"
            + "  { 'description': { '$regex': ?1, '$options': 'i' } },"
            + "  { 'placeId': { '$in': ?2 } },"
            + "  { 'supportsGroupToken': ?3 },"
            + "  { 'emergencySupport': ?4 },"
            + "  { 'isActive': ?5 }"
            + "]"
            + "}")
    Page<Service> advancedSearch(String name, String description, List<String> placeIds,
                                 Boolean supportsGroupToken, Boolean emergencySupport,
                                 Boolean isActive, Pageable pageable);

    // Search services with average service time filter
    @Query("{"
            + "'averageServiceTime': { '$lte': ?0 },"
            + "'isActive': true"
            + "}")
    List<Service> findByMaxServiceTime(Integer maxServiceTime);

    // Find services by place IDs with filters
    @Query("{"
            + "'placeId': { '$in': ?0 },"
            + "'supportsGroupToken': ?1,"
            + "'emergencySupport': ?2,"
            + "'isActive': true"
            + "}")
    List<Service> findByPlaceIdsWithFilters(List<String> placeIds, Boolean supportsGroupToken,
                                            Boolean emergencySupport);
}