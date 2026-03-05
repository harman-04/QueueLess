// src/main/java/com/queueless/backend/service/AlertConfigService.java
package com.queueless.backend.service;

import com.queueless.backend.exception.ResourceNotFoundException;
import com.queueless.backend.model.AlertConfig;
import com.queueless.backend.model.User;
import com.queueless.backend.repository.AlertConfigRepository;
import com.queueless.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertConfigService {

    private final AlertConfigRepository alertConfigRepository;
    private final UserRepository userRepository;

    public AlertConfig getConfig(String adminId) {
        return alertConfigRepository.findByAdminId(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Alert config not found for admin: " + adminId));
    }

    public AlertConfig createOrUpdateConfig(String adminId, int thresholdWaitTime, String notificationEmail) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        AlertConfig config = alertConfigRepository.findByAdminId(adminId)
                .orElse(new AlertConfig());

        config.setAdminId(adminId);
        config.setThresholdWaitTime(thresholdWaitTime);
        config.setNotificationEmail(notificationEmail != null ? notificationEmail : admin.getEmail());
        config.setEnabled(true);
        if (config.getCreatedAt() == null) {
            config.setCreatedAt(LocalDateTime.now());
        }
        config.setUpdatedAt(LocalDateTime.now());

        return alertConfigRepository.save(config);
    }

    public void deleteConfig(String adminId) {
        AlertConfig config = getConfig(adminId);
        alertConfigRepository.delete(config);
    }

    public void toggleEnabled(String adminId, boolean enabled) {
        AlertConfig config = getConfig(adminId);
        config.setEnabled(enabled);
        config.setUpdatedAt(LocalDateTime.now());
        alertConfigRepository.save(config);
    }
}