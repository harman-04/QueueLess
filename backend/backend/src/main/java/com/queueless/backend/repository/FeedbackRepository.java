// FeedbackRepository.java
package com.queueless.backend.repository;

import com.queueless.backend.model.Feedback;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeedbackRepository extends MongoRepository<Feedback, String> {
    List<Feedback> findByPlaceId(String placeId);
    List<Feedback> findByProviderId(String providerId);
    List<Feedback> findByQueueId(String queueId);
    List<Feedback> findByUserId(String userId);
    Optional<Feedback> findByTokenId(String tokenId);
    List<Feedback> findByPlaceIdAndRatingGreaterThan(String placeId, Integer minRating);
    long countByPlaceId(String placeId);
    long countByProviderId(String providerId);
}