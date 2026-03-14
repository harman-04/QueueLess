package com.queueless.backend.service;

import com.queueless.backend.model.AuditLog;
import com.queueless.backend.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public void logEvent(String action, String description, Map<String, Object> details) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = null;

        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            userId = authentication.getName();
        }

        AuditLog logEntry = new AuditLog();
        logEntry.setUserId(userId);
        logEntry.setAction(action);
        logEntry.setDescription(description);
        logEntry.setDetails(details);
        logEntry.setTimestamp(LocalDateTime.now());

        auditLogRepository.save(logEntry);
        log.debug("Audit log saved: {} - {}", action, description);
    }
}