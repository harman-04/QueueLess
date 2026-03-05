package com.queueless.backend.repository;

import com.queueless.backend.model.Place;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import java.util.List;

public interface PlaceRepository extends MongoRepository<Place, String> {
    List<Place> findByAdminId(String adminId);
    List<Place> findByType(String type);

    @Query("{ 'location' : { $near : { $geometry : { type : 'Point', coordinates: [?0, ?1] }, $maxDistance: ?2 } } }")
    List<Place> findByLocationNear(Double longitude, Double latitude, Double maxDistance);


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

    List<Place> findTopByOrderByRatingDesc(Pageable pageable);

}