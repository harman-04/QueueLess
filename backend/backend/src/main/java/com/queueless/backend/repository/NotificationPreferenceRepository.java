package com.queueless.backend.repository;

import com.queueless.backend.model.NotificationPreference;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationPreferenceRepository extends MongoRepository<NotificationPreference, String> {
    Optional<NotificationPreference> findByUserIdAndQueueId(String userId, String queueId);
    List<NotificationPreference> findByUserId(String userId);
    List<NotificationPreference> findByQueueId(String queueId);
    void deleteByUserIdAndQueueId(String userId, String queueId);
    void deleteByQueueId(String queueId);
}