package com.queueless.backend.repository;

import com.queueless.backend.model.Token;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface TokenRepository extends MongoRepository<Token, String> {
    Optional<Token> findByTokenValue(String tokenValue);
    Optional<Token> findByCreatedForEmail(String email);
}