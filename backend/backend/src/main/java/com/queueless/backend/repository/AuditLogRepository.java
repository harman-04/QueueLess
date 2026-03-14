package com.queueless.backend.repository;

import com.queueless.backend.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository extends MongoRepository<AuditLog, String> {
    Page<AuditLog> findByUserId(String userId, Pageable pageable);
    List<AuditLog> findByAction(String action);
    List<AuditLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
}