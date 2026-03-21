package com.queueless.backend.service;

import com.queueless.backend.model.AuditLog;
import com.queueless.backend.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private AuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        // Clear the security context before each test to avoid interference
        SecurityContextHolder.clearContext();
    }

    @Test
    void logEvent_WhenAuthenticated_ShouldSaveWithUserId() {
        // Arrange
        String userId = "user123";
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(userId);  // <-- add this
        when(authentication.getPrincipal()).thenReturn(userId);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        String action = "TEST_ACTION";
        String description = "Test description";
        Map<String, Object> details = Map.of("key", "value");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);

        // Act
        auditLogService.logEvent(action, description, details);

        // Assert
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();
        assertEquals(userId, saved.getUserId());
        assertEquals(action, saved.getAction());
        assertEquals(description, saved.getDescription());
        assertEquals(details, saved.getDetails());
        assertNotNull(saved.getTimestamp());
    }

    @Test
    void logEvent_WhenNotAuthenticated_ShouldSaveWithNullUserId() {
        // Arrange
        when(authentication.isAuthenticated()).thenReturn(false);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        String action = "TEST_ACTION";
        String description = "Test description";
        Map<String, Object> details = Map.of("key", "value");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);

        // Act
        auditLogService.logEvent(action, description, details);

        // Assert
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();
        assertNull(saved.getUserId());
        assertEquals(action, saved.getAction());
        assertEquals(description, saved.getDescription());
        assertEquals(details, saved.getDetails());
        assertNotNull(saved.getTimestamp());
    }

    @Test
    void logEvent_WhenAuthenticationIsNull_ShouldSaveWithNullUserId() {
        // Arrange – no authentication set in SecurityContext
        SecurityContextHolder.clearContext();

        String action = "TEST_ACTION";
        String description = "Test description";
        Map<String, Object> details = Map.of("key", "value");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);

        // Act
        auditLogService.logEvent(action, description, details);

        // Assert
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();
        assertNull(saved.getUserId());
        assertEquals(action, saved.getAction());
        assertEquals(description, saved.getDescription());
        assertEquals(details, saved.getDetails());
        assertNotNull(saved.getTimestamp());
    }
}