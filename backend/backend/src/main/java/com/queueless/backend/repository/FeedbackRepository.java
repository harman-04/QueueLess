// FeedbackRepository.java
package com.queueless.backend.repository;

import com.queueless.backend.model.Feedback;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;


public interface FeedbackRepository extends MongoRepository<Feedback, String> {
    List<Feedback> findByPlaceId(String placeId);
    List<Feedback> findByProviderId(String providerId);
    Optional<Feedback> findByTokenId(String tokenId);
    List<Feedback> findTopByOrderByCreatedAtDesc(Pageable pageable);
    List<Feedback> findByTokenIdIn(Collection<String> tokenIds);
   }