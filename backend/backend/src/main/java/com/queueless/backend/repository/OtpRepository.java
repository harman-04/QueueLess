package com.queueless.backend.repository;

import com.queueless.backend.model.OtpDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface OtpRepository extends MongoRepository<OtpDocument, String> {
    Optional<OtpDocument> findByEmail(String email);
    void deleteByEmail(String email);
}
