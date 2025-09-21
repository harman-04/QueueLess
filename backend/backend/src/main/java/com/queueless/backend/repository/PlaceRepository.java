package com.queueless.backend.repository;

import com.queueless.backend.model.Place;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
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

    List<Place> findAllById(List<String> ids);


    // Advanced search with multiple filters
    @Query("{"
            + "'$and': ["
            + "  { 'name': { '$regex': ?0, '$options': 'i' } },"
            + "  { 'type': { '$regex': ?1, '$options': 'i' } },"
            + "  { 'rating': { '$gte': ?2 } },"
            + "  { 'isActive': ?3 }"
            + "]"
            + "}")
    Page<Place> advancedSearch(String name, String type, Double minRating, Boolean isActive, Pageable pageable);

    // Search by multiple criteria with location
    @Query("{"
            + "'$and': ["
            + "  { 'name': { '$regex': ?0, '$options': 'i' } },"
            + "  { 'type': { '$in': ?1 } },"
            + "  { 'rating': { '$gte': ?2 } },"
            + "  { 'isActive': true },"
            + "  { 'location': {"
            + "    '$near': {"
            + "      '$geometry': {"
            + "        'type': 'Point',"
            + "        'coordinates': [?3, ?4]"
            + "      },"
            + "      '$maxDistance': ?5"
            + "    }"
            + "  } }"
            + "]"
            + "}")
    List<Place> searchNearbyWithFilters(String name, List<String> types, Double minRating,
                                        Double longitude, Double latitude, Double maxDistance);

    // Search places by service type
    @Aggregation(pipeline = {
            "{ '$lookup': {"
                    + "  'from': 'services',"
                    + "  'localField': '_id',"
                    + "  'foreignField': 'placeId',"
                    + "  'as': 'services'"
                    + "} }",
            "{ '$match': {"
                    + "  'services.name': { '$regex': ?0, '$options': 'i' }"
                    + "} }",
            "{ '$match': {"
                    + "  'isActive': true"
                    + "} }"
    })
    List<Place> findByServiceName(String serviceName);

    // Get distinct types for filter options
    @Query(value = "{}", fields = "{ 'type' : 1 }")
    List<Place> findDistinctTypes();

    // Count places by filter criteria
    @Query("{"
            + "'$and': ["
            + "  { 'name': { '$regex': ?0, '$options': 'i' } },"
            + "  { 'type': { '$regex': ?1, '$options': 'i' } },"
            + "  { 'rating': { '$gte': ?2 } },"
            + "  { 'isActive': ?3 }"
            + "]"
            + "}")
    Long countByFilters(String name, String type, Double minRating, Boolean isActive);



    // New, dedicated method for favorite places that correctly handles ObjectId conversion
    List<Place> findFavoritesByIdIn(List<ObjectId> ids);


}