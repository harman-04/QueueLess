package com.queueless.backend.service;

import com.queueless.backend.exception.ResourceNotFoundException;
import com.queueless.backend.model.AlertConfig;
import com.queueless.backend.model.User;
import com.queueless.backend.repository.AlertConfigRepository;
import com.queueless.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertConfigServiceTest {

    @Mock
    private AlertConfigRepository alertConfigRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AlertConfigService alertConfigService;

    private final String adminId = "admin123";
    private final String adminEmail = "admin@example.com";
    private User adminUser;
    private AlertConfig existingConfig;

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
                .id(adminId)
                .email(adminEmail)
                .build();

        existingConfig = new AlertConfig();
        existingConfig.setId("config1");
        existingConfig.setAdminId(adminId);
        existingConfig.setThresholdWaitTime(15);
        existingConfig.setNotificationEmail(adminEmail);
        existingConfig.setEnabled(true);
        existingConfig.setCreatedAt(LocalDateTime.now().minusDays(1));
        existingConfig.setUpdatedAt(LocalDateTime.now().minusDays(1));
    }

    // ==================== getConfig ====================

    @Test
    void getConfig_Success() {
        when(alertConfigRepository.findByAdminId(adminId)).thenReturn(Optional.of(existingConfig));

        AlertConfig result = alertConfigService.getConfig(adminId);

        assertNotNull(result);
        assertEquals(adminId, result.getAdminId());
        assertEquals(15, result.getThresholdWaitTime());
        verify(alertConfigRepository).findByAdminId(adminId);
    }

    @Test
    void getConfig_NotFound() {
        when(alertConfigRepository.findByAdminId(adminId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> alertConfigService.getConfig(adminId));
        verify(alertConfigRepository).findByAdminId(adminId);
    }

    // ==================== createOrUpdateConfig ====================

    @Test
    void createOrUpdateConfig_NewConfig() {
        when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
        when(alertConfigRepository.findByAdminId(adminId)).thenReturn(Optional.empty());
        when(alertConfigRepository.save(any(AlertConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        AlertConfig result = alertConfigService.createOrUpdateConfig(adminId, 30, null);

        assertNotNull(result);
        assertEquals(adminId, result.getAdminId());
        assertEquals(30, result.getThresholdWaitTime());
        assertEquals(adminEmail, result.getNotificationEmail()); // falls back to admin email
        assertTrue(result.isEnabled());
        assertNotNull(result.getCreatedAt());
        assertNotNull(result.getUpdatedAt());

        verify(userRepository).findById(adminId);
        verify(alertConfigRepository).findByAdminId(adminId);
        verify(alertConfigRepository).save(any(AlertConfig.class));
    }

    @Test
    void createOrUpdateConfig_UpdateExisting() {
        when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
        when(alertConfigRepository.findByAdminId(adminId)).thenReturn(Optional.of(existingConfig));
        when(alertConfigRepository.save(any(AlertConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        AlertConfig result = alertConfigService.createOrUpdateConfig(adminId, 45, "custom@example.com");

        assertNotNull(result);
        assertEquals(adminId, result.getAdminId());
        assertEquals(45, result.getThresholdWaitTime());
        assertEquals("custom@example.com", result.getNotificationEmail());
        assertTrue(result.isEnabled());
        assertNotNull(result.getCreatedAt()); // should preserve old createdAt
        assertNotNull(result.getUpdatedAt());

        verify(userRepository).findById(adminId);
        verify(alertConfigRepository).findByAdminId(adminId);
        verify(alertConfigRepository).save(any(AlertConfig.class));
    }

    @Test
    void createOrUpdateConfig_AdminNotFound() {
        when(userRepository.findById(adminId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> alertConfigService.createOrUpdateConfig(adminId, 30, null));
        verify(userRepository).findById(adminId);
        verifyNoInteractions(alertConfigRepository);
    }

    // ==================== deleteConfig ====================

    @Test
    void deleteConfig_Success() {
        when(alertConfigRepository.findByAdminId(adminId)).thenReturn(Optional.of(existingConfig));
        doNothing().when(alertConfigRepository).delete(existingConfig);

        assertDoesNotThrow(() -> alertConfigService.deleteConfig(adminId));

        verify(alertConfigRepository).findByAdminId(adminId);
        verify(alertConfigRepository).delete(existingConfig);
    }

    @Test
    void deleteConfig_NotFound() {
        when(alertConfigRepository.findByAdminId(adminId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> alertConfigService.deleteConfig(adminId));
        verify(alertConfigRepository).findByAdminId(adminId);
        verify(alertConfigRepository, never()).delete(any());
    }

    // ==================== toggleEnabled ====================

    @Test
    void toggleEnabled_Success() {
        when(alertConfigRepository.findByAdminId(adminId)).thenReturn(Optional.of(existingConfig));
        when(alertConfigRepository.save(any(AlertConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        alertConfigService.toggleEnabled(adminId, false);

        assertFalse(existingConfig.isEnabled());
        assertNotNull(existingConfig.getUpdatedAt());

        verify(alertConfigRepository).findByAdminId(adminId);
        verify(alertConfigRepository).save(existingConfig);
    }

    @Test
    void toggleEnabled_NotFound() {
        when(alertConfigRepository.findByAdminId(adminId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> alertConfigService.toggleEnabled(adminId, true));
        verify(alertConfigRepository).findByAdminId(adminId);
        verify(alertConfigRepository, never()).save(any());
    }
}