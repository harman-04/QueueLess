// src/main/java/com/queueless/backend/repository/AlertConfigRepository.java
package com.queueless.backend.repository;

import com.queueless.backend.model.AlertConfig;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface AlertConfigRepository extends MongoRepository<AlertConfig, String> {
    Optional<AlertConfig> findByAdminId(String adminId);
}