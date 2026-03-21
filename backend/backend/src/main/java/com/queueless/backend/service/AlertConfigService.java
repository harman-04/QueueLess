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
        log.debug("Fetching alert config for admin: {}", adminId);
        return alertConfigRepository.findByAdminId(adminId)
                .orElseThrow(() -> {
                    log.info("No alert config found for admin: {}", adminId);
                    return new ResourceNotFoundException("Alert config not found for admin: " + adminId);
                });
    }

    public AlertConfig createOrUpdateConfig(String adminId, int thresholdWaitTime, String notificationEmail) {
        log.info("Creating/updating alert config for admin: {} with threshold={}, email={}", adminId, thresholdWaitTime, notificationEmail);
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> {
                    log.error("Admin not found with ID: {}", adminId);
                    return new ResourceNotFoundException("Admin not found");
                });

        AlertConfig config = alertConfigRepository.findByAdminId(adminId)
                .orElseGet(() -> {
                    log.debug("No existing config, creating new one for admin: {}", adminId);
                    return new AlertConfig();
                });

        config.setAdminId(adminId);
        config.setThresholdWaitTime(thresholdWaitTime);
        config.setNotificationEmail(notificationEmail != null ? notificationEmail : admin.getEmail());
        config.setEnabled(true);
        if (config.getCreatedAt() == null) {
            config.setCreatedAt(LocalDateTime.now());
            log.debug("Setting createdAt for new config");
        }
        config.setUpdatedAt(LocalDateTime.now());

        AlertConfig saved = alertConfigRepository.save(config);
        log.info("Alert config saved for admin: {} with ID: {}", adminId, saved.getId());
        return saved;
    }

    public void deleteConfig(String adminId) {
        log.warn("Deleting alert config for admin: {}", adminId);
        AlertConfig config = getConfig(adminId);
        alertConfigRepository.delete(config);
        log.info("Alert config deleted for admin: {}", adminId);
    }

    public void toggleEnabled(String adminId, boolean enabled) {
        log.info("Toggling alerts for admin: {} to enabled={}", adminId, enabled);
        AlertConfig config = getConfig(adminId);
        config.setEnabled(enabled);
        config.setUpdatedAt(LocalDateTime.now());
        alertConfigRepository.save(config);
        log.info("Alert config toggled for admin: {}", adminId);
    }
}