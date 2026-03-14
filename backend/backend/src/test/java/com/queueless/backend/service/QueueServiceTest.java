package com.queueless.backend.service;

import com.queueless.backend.dto.*;
import com.queueless.backend.enums.Role;
import com.queueless.backend.enums.TokenStatus;
import com.queueless.backend.exception.QueueInactiveException;
import com.queueless.backend.exception.ResourceNotFoundException;
import com.queueless.backend.exception.UserAlreadyInQueueException;
import com.queueless.backend.model.*;
import com.queueless.backend.model.Queue;
import com.queueless.backend.repository.FeedbackRepository;
import com.queueless.backend.repository.QueueRepository;
import com.queueless.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.queueless.backend.exception.AccessDeniedException;
import com.queueless.backend.model.*;
import com.queueless.backend.model.Queue;
import com.queueless.backend.repository.QueueHourlyStatsRepository;

@ExtendWith(MockitoExtension.class)
class QueueServiceTest {

    @Mock
    private QueueRepository queueRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PlaceService placeService;

    @Mock
    private ServiceService serviceService;

    @Mock
    private FeedbackService feedbackService;

    @Mock
    private FeedbackRepository feedbackRepository;

    @Mock
    private QueueHourlyStatsRepository statsRepository;

    @Mock
    private ExportService exportService;

    @Mock
    private ExportCacheService exportCacheService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private QueueService queueService;

    private Queue testQueue;
    private User testUser;
    private final String queueId = "queue123";
    private final String userId = "user123";
    private final String providerId = "provider123";
    private final String placeId = "place123";
    private final String serviceId = "service123";

    @BeforeEach
    void setUp() {
        testQueue = new Queue(providerId, "Test Service", placeId, serviceId);
        testQueue.setId(queueId);
        testQueue.setIsActive(true);
        testQueue.setMaxCapacity(10);
        testQueue.setSupportsGroupToken(true);
        testQueue.setEmergencySupport(true);
        testQueue.setAutoApproveEmergency(false);
        testQueue.setEmergencyPriorityWeight(10);
        testQueue.setTokens(new ArrayList<>());
        testQueue.setPendingEmergencyTokens(new ArrayList<>());
        testQueue.setTokenCounter(0);

        testUser = User.builder()
                .id(userId)
                .name("Test User")
                .email("test@example.com")
                .role(Role.USER)
                .activeTokenId(null)
                .lastQueueJoinTime(null)
                .build();
    }

    private QueueToken createTestToken(String tokenId, String status) {
        return new QueueToken(tokenId, userId, testUser.getName(), status, LocalDateTime.now());
    }

    // ================= ADD NEW TOKEN =================

    @Test
    void addNewTokenSuccess() {
        when(queueRepository.findById(queueId)).thenReturn(Optional.of(testQueue));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(queueRepository.save(any(Queue.class))).thenAnswer(inv -> inv.getArgument(0));

        QueueToken token = queueService.addNewToken(queueId, userId);

        assertNotNull(token);
        assertEquals("queue123-T-001", token.getTokenId());

        verify(queueRepository).findById(queueId);
        // Updated to times(2) because getUserOrThrow is called twice in the current logic
        verify(userRepository, times(2)).findById(userId);
        verify(userRepository).save(any(User.class));
        verify(queueRepository).save(any(Queue.class));
    }

    @Test
    void addNewTokenQueueInactive() {
        testQueue.setIsActive(false);
        when(queueRepository.findById(queueId)).thenReturn(Optional.of(testQueue));

        assertThrows(QueueInactiveException.class, () -> queueService.addNewToken(queueId, userId));
        verify(queueRepository, never()).save(any());
    }

    @Test
    void addNewTokenUserAlreadyHasActiveToken() {
        testUser.setActiveTokenId("T-001");
        when(queueRepository.findById(queueId)).thenReturn(Optional.of(testQueue));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        assertThrows(UserAlreadyInQueueException.class, () -> queueService.addNewToken(queueId, userId));
        verify(queueRepository, never()).save(any());
    }

    @Test
    void addNewTokenUserAlreadyInThisQueue() {
        QueueToken existingToken = createTestToken("T-001", TokenStatus.WAITING.toString());
        testQueue.getTokens().add(existingToken);
        when(queueRepository.findById(queueId)).thenReturn(Optional.of(testQueue));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        assertThrows(UserAlreadyInQueueException.class, () -> queueService.addNewToken(queueId, userId));
        verify(queueRepository, never()).save(any());
    }

    @Test
    void addNewTokenQueueFull() {
        for (int i = 0; i < 10; i++) {
            testQueue.getTokens().add(new QueueToken("T-" + i, "otherUser" + i, "Other",
                    TokenStatus.WAITING.toString(), LocalDateTime.now()));
        }

        when(queueRepository.findById(queueId)).thenReturn(Optional.of(testQueue));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        assertThrows(com.queueless.backend.exception.QueueFullException.class,
                () -> queueService.addNewToken(queueId, userId));

        verify(queueRepository, never()).save(any());
    }

    // ================= ADD NEW TOKEN WITH DETAILS =================

    @Test
    void addNewTokenWithDetailsSuccess() {
        TokenRequestDTO details = new TokenRequestDTO();
        details.setPurpose("Consultation");
        details.setCondition("Fever");
        details.setNotes("Bring ID");
        details.setIsPrivate(false);
        details.setVisibleToProvider(true);
        details.setVisibleToAdmin(true);

        when(queueRepository.findById(queueId)).thenReturn(Optional.of(testQueue));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(queueRepository.save(any(Queue.class))).thenAnswer(inv -> inv.getArgument(0));

        QueueToken token = queueService.addNewTokenWithDetails(queueId, userId, details);

        assertNotNull(token);
        assertEquals("queue123-T-001", token.getTokenId());
        assertEquals(TokenStatus.WAITING.toString(), token.getStatus());
        assertNotNull(token.getUserDetails());
        assertEquals("Consultation", token.getUserDetails().getPurpose());
        assertEquals("Fever", token.getUserDetails().getCondition());
        assertEquals("Bring ID", token.getUserDetails().getNotes());
        assertFalse(token.getUserDetails().getIsPrivate());

        verify(userRepository).save(argThat(user -> user.getActiveTokenId().equals(token.getTokenId())));
    }

    // ================= ADD GROUP TOKEN =================

    @Test
    void addGroupTokenSuccess() {
        List<QueueToken.GroupMember> members = List.of(
                new QueueToken.GroupMember("Member1", "Details1"),
                new QueueToken.GroupMember("Member2", "Details2")
        );

        when(queueRepository.findById(queueId)).thenReturn(Optional.of(testQueue));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(queueRepository.save(any(Queue.class))).thenAnswer(inv -> inv.getArgument(0));

        QueueToken token = queueService.addGroupToken(queueId, userId, members);

        assertNotNull(token);
        assertEquals("queue123-G-001", token.getTokenId());
        assertTrue(token.getIsGroup());
        assertEquals(2, token.getGroupSize());
        assertEquals(members, token.getGroupMembers());
        verify(userRepository).save(argThat(user -> user.getActiveTokenId().equals(token.getTokenId())));
    }

    @Test
    void addGroupTokenQueueDoesNotSupportGroup() {
        testQueue.setSupportsGroupToken(false);
        when(queueRepository.findById(queueId)).thenReturn(Optional.of(testQueue));

        assertThrows(UnsupportedOperationException.class,
                () -> queueService.addGroupToken(queueId, userId, List.of()));
    }

    @Test
    void addGroupTokenInsufficientMembers() {
        when(queueRepository.findById(queueId)).thenReturn(Optional.of(testQueue));

        assertThrows(IllegalArgumentException.class,
                () -> queueService.addGroupToken(queueId, userId, List.of()));
    }

    // ================= ADD EMERGENCY TOKEN =================

    @Test
    void addEmergencyTokenAutoApproved() {
        testQueue.setAutoApproveEmergency(true);
        String emergencyDetails = "Severe pain";

        when(queueRepository.findById(queueId)).thenReturn(Optional.of(testQueue));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(queueRepository.save(any(Queue.class))).thenAnswer(inv -> inv.getArgument(0));

        QueueToken token = queueService.addEmergencyToken(queueId, userId, emergencyDetails);

        assertNotNull(token);
        assertEquals("queue123-E-001", token.getTokenId());
        assertTrue(token.getIsEmergency());
        assertEquals(emergencyDetails, token.getEmergencyDetails());
        assertEquals(10, token.getPriority());
        assertEquals(TokenStatus.WAITING.toString(), token.getStatus());
        verify(userRepository).save(argThat(user -> user.getActiveTokenId().equals(token.getTokenId())));
    }

    @Test
    void addEmergencyTokenPendingApproval() {
        String emergencyDetails = "Severe pain";

        when(queueRepository.findById(queueId)).thenReturn(Optional.of(testQueue));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(queueRepository.save(any(Queue.class))).thenAnswer(inv -> inv.getArgument(0));

        QueueToken token = queueService.addEmergencyToken(queueId, userId, emergencyDetails);

        assertNotNull(token);
        assertEquals("queue123-E-001", token.getTokenId());
        assertEquals(TokenStatus.PENDING.toString(), token.getStatus());
        assertEquals(1, testQueue.getPendingEmergencyTokens().size());
        verify(userRepository, never()).save(any()); // user not activated yet
    }

    @Test
    void addEmergencyTokenQueueDoesNotSupportEmergency() {
        testQueue.setEmergencySupport(false);
        when(queueRepository.findById(queueId)).thenReturn(Optional.of(testQueue));

        assertThrows(UnsupportedOperationException.class,
                () -> queueService.addEmergencyToken(queueId, userId, "details"));
    }

    // ================= APPROVE EMERGENCY TOKEN =================

    @Test
    void approveEmergencyTokenSuccess() {
        QueueToken pending = new QueueToken("E-001", userId, testUser.getName(), TokenStatus.PENDING.toString(),
                LocalDateTime.now(), "Emergency", 10);
        testQueue.getPendingEmergencyTokens().add(pending);

        when(queueRepository.findById(queueId)).thenReturn(Optional.of(testQueue));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(queueRepository.save(any(Queue.class))).thenAnswer(inv -> inv.getArgument(0));

        Queue updated = queueService.approveEmergencyToken(queueId, "E-001", true, null);

        assertTrue(updated.getPendingEmergencyTokens().isEmpty());
        Optional<QueueToken> approved = updated.getTokens().stream()
                .filter(t -> t.getTokenId().equals("E-001")).findFirst();
        assertTrue(approved.isPresent());
        assertEquals(TokenStatus.WAITING.toString(), approved.get().getStatus());
        verify(userRepository).save(argThat(user -> user.getActiveTokenId().equals("E-001")));
        verify(messagingTemplate).convertAndSendToUser(eq(userId), eq("/queue/emergency-approved"), any(Map.class));
    }

    @Test
    void approveEmergencyTokenReject() {
        QueueToken pending = new QueueToken("E-001", userId, testUser.getName(), TokenStatus.PENDING.toString(),
                LocalDateTime.now(), "Emergency", 10);
        testQueue.getPendingEmergencyTokens().add(pending);

        when(queueRepository.findById(queueId)).thenReturn(Optional.of(testQueue));
        when(queueRepository.save(any(Queue.class))).thenAnswer(inv -> inv.getArgument(0));

        Queue updated = queueService.approveEmergencyToken(queueId, "E-001", false, "No staff available");

        assertTrue(updated.getPendingEmergencyTokens().isEmpty());
        assertTrue(updated.getTokens().stream().noneMatch(t -> t.getTokenId().equals("E-001")));
        verify(userRepository, never()).save(any());
        verify(messagingTemplate).convertAndSendToUser(eq(userId), eq("/queue/emergency-approved"), any(Map.class));
    }

    @Test
    void approveEmergencyTokenNotFound() {
        when(queueRepository.findById(queueId)).thenReturn(Optional.of(testQueue));

        assertThrows(ResourceNotFoundException.class,
                () -> queueService.approveEmergencyToken(queueId, "E-001", true, null));
    }

    // ================= GET PENDING EMERGENCY TOKENS =================

    @Test
    void getPendingEmergencyTokensSuccess() {
        QueueToken pending = new QueueToken("E-001", userId, testUser.getName(), TokenStatus.PENDING.toString(),
                LocalDateTime.now(), "Emergency", 10);
        testQueue.getPendingEmergencyTokens().add(pending);

        when(queueRepository.findById(queueId)).thenReturn(Optional.of(testQueue));

        List<QueueToken> result = queueService.getPendingEmergencyTokens(queueId);

        assertEquals(1, result.size());
        assertEquals("E-001", result.get(0).getTokenId());
    }

    // ================= SERVE NEXT TOKEN =================

    @Test
    void serveNextTokenWithNoInServiceToken() {
        List<QueueToken> waiting = Arrays.asList(
                createTestToken("T-001", TokenStatus.WAITING.toString()),
                createTestToken("T-002", TokenStatus.WAITING.toString())
        );
        testQueue.setTokens(waiting);

        when(queueRepository.findById(queueId)).thenReturn(Optional.of(testQueue));
        when(queueRepository.save(any(Queue.class))).thenAnswer(inv -> inv.getArgument(0));

        Queue updated = queueService.serveNextToken(queueId);

        Optional<QueueToken> inService = updated.getTokens().stream()
                .filter(t -> TokenStatus.IN_SERVICE.toString().equals(t.getStatus())).findFirst();
        assertTrue(inService.isPresent());
        assertEquals("T-001", inService.get().getTokenId());
        verify(messagingTemplate).convertAndSend(eq("/topic/queues/" + queueId), any(Queue.class));
    }

    @Test
    void serveNextTokenWithExistingInServiceToken() {
        QueueToken inService = createTestToken("T-001", TokenStatus.IN_SERVICE.toString());
        QueueToken waiting = createTestToken("T-002", TokenStatus.WAITING.toString());
        testQueue.setTokens(Arrays.asList(inService, waiting));

        when(queueRepository.findById(queueId)).thenReturn(Optional.of(testQueue));
        // The method will call getUserOrThrow for the user of the in-service token (userId)
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(queueRepository.save(any(Queue.class))).thenAnswer(inv -> inv.getArgument(0));

        Queue updated = queueService.serveNextToken(queueId);

        // Previous in-service should be completed
        Optional<QueueToken> completed = updated.getTokens().stream()
                .filter(t -> TokenStatus.COMPLETED.toString().equals(t.getStatus()) &&
                        t.getTokenId().equals("T-001")).findFirst();
        assertTrue(completed.isPresent());

        // New token should be in service
        Optional<QueueToken> newInService = updated.getTokens().stream()
                .filter(t -> TokenStatus.IN_SERVICE.toString().equals(t.getStatus()) &&
                        t.getTokenId().equals("T-002")).findFirst();
        assertTrue(newInService.isPresent());

        // Verify user's active token was cleared for the completed token
        verify(userRepository).save(argThat(user -> user.getActiveTokenId() == null));
    }

    @Test
    void serveNextTokenNoWaitingTokens() {
        when(queueRepository.findById(queueId)).thenReturn(Optional.of(testQueue));

        Queue updated = queueService.serveNextToken(queueId);

        assertSame(testQueue, updated);
        verify(queueRepository, never()).save(any());
    }

    // ================= COMPLETE TOKEN =================

    @Test
    void completeTokenSuccess() {
        QueueToken token = createTestToken("T-001", TokenStatus.IN_SERVICE.toString());
        token.setServedAt(LocalDateTime.now().minusMinutes(5));
        testQueue.getTokens().add(token);

        when(queueRepository.findById(queueId)).thenReturn(Optional.of(testQueue));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(queueRepository.save(any(Queue.class))).thenAnswer(inv -> inv.getArgument(0));

        Queue updated = queueService.completeToken(queueId, "T-001");

        Optional<QueueToken> completed = updated.getTokens().stream()
                .filter(t -> t.getTokenId().equals("T-001")).findFirst();
        assertTrue(completed.isPresent());
        assertEquals(TokenStatus.COMPLETED.toString(), completed.get().getStatus());
        assertNotNull(completed.get().getCompletedAt());
        assertNotNull(completed.get().getServiceDurationMinutes());
        verify(userRepository).save(argThat(user -> user.getActiveTokenId() == null));
    }

    @Test
    void completeTokenNotFound() {
        when(queueRepository.findById(queueId)).thenReturn(Optional.of(testQueue));

        assertThrows(ResourceNotFoundException.class,
                () -> queueService.completeToken(queueId, "T-001"));
    }

    // ================= CANCEL TOKEN =================






    // ================= REORDER QUEUE =================

    @Test
    void reorderQueueSuccess() {
        List<QueueToken> oldTokens = Arrays.asList(
                createTestToken("T-001", TokenStatus.WAITING.toString()),
                createTestToken("T-002", TokenStatus.WAITING.toString())
        );
        testQueue.setTokens(oldTokens);

        List<QueueToken> newOrder = Arrays.asList(oldTokens.get(1), oldTokens.get(0));
        when(queueRepository.findById(queueId)).thenReturn(Optional.of(testQueue));
        when(queueRepository.save(any(Queue.class))).thenAnswer(inv -> inv.getArgument(0));

        Queue updated = queueService.reorderQueue(queueId, newOrder);

        assertEquals(newOrder, updated.getTokens());
        verify(messagingTemplate).convertAndSend(eq("/topic/queues/" + queueId), any(Queue.class));
    }

    // ================= RESET QUEUE WITH OPTIONS =================

    @Test
    void resetQueueWithOptionsSuccessWithoutPreserve() {
        QueueResetRequestDTO request = new QueueResetRequestDTO();
        request.setPreserveData(false);

        testQueue.getTokens().add(createTestToken("T-001", TokenStatus.WAITING.toString()));
        testQueue.getTokens().add(createTestToken("T-002", TokenStatus.WAITING.toString()));

        when(queueRepository.findById(queueId)).thenReturn(Optional.of(testQueue));
        when(userRepository.findById(providerId)).thenReturn(Optional.of(User.builder().id(providerId).role(Role.PROVIDER).build()));
        when(queueRepository.save(any(Queue.class))).thenAnswer(inv -> inv.getArgument(0));

        QueueResetResponseDTO response = queueService.resetQueueWithOptions(queueId, request, providerId);

        assertTrue(response.getSuccess());
        assertEquals(2, response.getTokensReset());
        assertNull(response.getExportFileUrl());
        verify(queueRepository).save(argThat(queue -> queue.getTokens().isEmpty()));
    }

    // ================= CANCEL TOKEN =================

    @Test
    void cancelTokenFromWaitingSuccess() {
        // Arrange
        String reason = "Provider requested cancellation";
        QueueToken token = createTestToken("T-001", TokenStatus.WAITING.toString());
        testUser.setActiveTokenId("T-001");
        testQueue.getTokens().add(token);

        when(queueRepository.findById(queueId)).thenReturn(Optional.of(testQueue));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(queueRepository.save(any(Queue.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Queue updated = queueService.cancelToken(queueId, "T-001", reason);

        // Assert – token should still exist with status CANCELLED
        Optional<QueueToken> cancelledToken = updated.getTokens().stream()
                .filter(t -> t.getTokenId().equals("T-001"))
                .findFirst();
        assertTrue(cancelledToken.isPresent());
        assertEquals(TokenStatus.CANCELLED.toString(), cancelledToken.get().getStatus());
        assertEquals(reason, cancelledToken.get().getCancellationReason());

        // Verify User state was cleared
        verify(userRepository).save(argThat(user -> user.getActiveTokenId() == null));

        // Verify WebSocket notification was sent
        verify(messagingTemplate).convertAndSendToUser(
                eq(userId),
                eq("/queue/token-cancelled"),
                argThat(map -> ((Map)map).get("reason").equals(reason))
        );
    }

    @Test
    void cancelTokenFromPendingEmergencySuccess() {
        // Arrange
        String reason = "Emergency not valid";
        QueueToken token = new QueueToken(
                "E-001",
                userId,
                testUser.getName(),
                TokenStatus.PENDING.toString(),
                LocalDateTime.now(),
                "Emergency",
                10
        );

        testQueue.getPendingEmergencyTokens().add(token);

        when(queueRepository.findById(queueId)).thenReturn(Optional.of(testQueue));
        when(queueRepository.save(any(Queue.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Queue updated = queueService.cancelToken(queueId, "E-001", reason);

        // Assert
        assertTrue(updated.getPendingEmergencyTokens().isEmpty());

        // ❗ No notification should be sent for pending tokens
        verify(messagingTemplate, never()).convertAndSendToUser(
                anyString(),
                eq("/queue/token-cancelled"),
                anyMap()
        );
    }

    @Test
    void cancelTokenNotFound() {
        // Arrange
        when(queueRepository.findById(queueId)).thenReturn(Optional.of(testQueue));

        // Act & Assert
        // Added the reason parameter to match method signature
        assertThrows(ResourceNotFoundException.class,
                () -> queueService.cancelToken(queueId, "T-001", "Random Reason"));
    }

    @Test
    void resetQueueWithOptionsSuccessWithPreserve() throws Exception {
        QueueResetRequestDTO request = new QueueResetRequestDTO();
        request.setPreserveData(true);
        request.setReportType("full");
        request.setIncludeUserDetails(true);

        testQueue.getTokens().add(createTestToken("T-001", TokenStatus.WAITING.toString()));

        when(queueRepository.findById(queueId)).thenReturn(Optional.of(testQueue));
        when(userRepository.findById(providerId)).thenReturn(Optional.of(User.builder().id(providerId).role(Role.PROVIDER).build()));
        when(exportService.exportQueueToPdf(eq(testQueue), eq("full"), eq(true))).thenReturn(new byte[]{1,2,3});
        when(queueRepository.save(any(Queue.class))).thenAnswer(inv -> inv.getArgument(0));

        QueueResetResponseDTO response = queueService.resetQueueWithOptions(queueId, request, providerId);

        assertTrue(response.getSuccess());
        assertEquals(1, response.getTokensReset());
        assertNotNull(response.getExportFileUrl());
        verify(exportCacheService).saveExport(anyString(), any(byte[].class), anyString(), eq(queueId), eq("full"), eq("pdf"));
    }

    @Test
    void resetQueueWithOptionsAccessDenied() {
        QueueResetRequestDTO request = new QueueResetRequestDTO();
        request.setPreserveData(false);

        when(queueRepository.findById(queueId)).thenReturn(Optional.of(testQueue));
        when(userRepository.findById("otherUser")).thenReturn(Optional.of(User.builder().id("otherUser").role(Role.USER).build()));

        assertThrows(AccessDeniedException.class,
                () -> queueService.resetQueueWithOptions(queueId, request, "otherUser"));
    }

// ================= GET USER DETAILS FOR TOKEN =================

    @Test
    void getUserDetailsForTokenAsOwner() {
        QueueToken token = createTestToken("T-001", TokenStatus.WAITING.toString());
        UserQueueDetails details = new UserQueueDetails();
        details.setPurpose("Test");
        details.setCondition("None");
        details.setNotes("Note");
        details.setIsPrivate(false);
        token.setUserDetails(details);
        testQueue.getTokens().add(token);

        when(queueRepository.findById(queueId)).thenReturn(Optional.of(testQueue));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        UserDetailsResponseDTO response = queueService.getUserDetailsForToken(queueId, "T-001", userId, Role.USER);

        assertTrue(response.getDetailsVisible());
        assertEquals(userId, response.getUserId());
        assertEquals(testUser.getName(), response.getUserName());
        assertEquals("Test", response.getPurpose());
        assertEquals("None", response.getCondition());
        assertEquals("Note", response.getNotes());
    }

    @Test
    void getUserDetailsForTokenAsProvider() {
        QueueToken token = createTestToken("T-001", TokenStatus.WAITING.toString());
        UserQueueDetails details = new UserQueueDetails();
        details.setPurpose("Test");
        details.setVisibleToProvider(true);
        token.setUserDetails(details);
        testQueue.getTokens().add(token);

        when(queueRepository.findById(queueId)).thenReturn(Optional.of(testQueue));
        when(userRepository.findById(providerId)).thenReturn(Optional.of(User.builder().id(providerId).role(Role.PROVIDER).build()));

        UserDetailsResponseDTO response = queueService.getUserDetailsForToken(queueId, "T-001", providerId, Role.PROVIDER);

        assertTrue(response.getDetailsVisible());
        assertEquals("Test", response.getPurpose());
    }

    @Test
    void getUserDetailsForTokenPrivateNotVisible() {
        QueueToken token = createTestToken("T-001", TokenStatus.WAITING.toString());
        UserQueueDetails details = new UserQueueDetails();
        details.setPurpose("Test");
        details.setIsPrivate(true);
        token.setUserDetails(details);
        testQueue.getTokens().add(token);

        when(queueRepository.findById(queueId)).thenReturn(Optional.of(testQueue));
        when(userRepository.findById(providerId)).thenReturn(Optional.of(User.builder().id(providerId).role(Role.PROVIDER).build()));

        UserDetailsResponseDTO response = queueService.getUserDetailsForToken(queueId, "T-001", providerId, Role.PROVIDER);

        assertTrue(response.getDetailsVisible()); // still visible because provider can see but private means fields hidden
        assertNull(response.getPurpose()); // purpose should be hidden
    }

    @Test
    void getUserDetailsForTokenNotFound() {
        when(queueRepository.findById(queueId)).thenReturn(Optional.of(testQueue));

        assertThrows(ResourceNotFoundException.class,
                () -> queueService.getUserDetailsForToken(queueId, "T-001", userId, Role.USER));
    }

// ================= CREATE NEW QUEUE =================
// ================= CREATE NEW QUEUE =================

    @Test
    void createNewQueueSimple() {
        // This method ONLY uses queueRepository, so we remove the other stubs
        when(queueRepository.save(any(Queue.class))).thenAnswer(inv -> inv.getArgument(0));

        Queue created = queueService.createNewQueue(providerId, "Test", placeId, serviceId);

        assertNotNull(created);
        assertEquals(providerId, created.getProviderId());
        assertEquals("Test", created.getServiceName());

        // Verify that the simple method didn't touch the services it didn't need
        verify(queueRepository).save(any(Queue.class));
        verifyNoInteractions(userRepository, placeService, serviceService);
    }

    @Test
    void createNewQueueWithAdvancedSettings() {
        // 1. Setup a Place with an adminId to avoid NPE
        Place place = new Place();
        place.setId(placeId);
        place.setAdminId("admin123");

        when(userRepository.findById(providerId)).thenReturn(Optional.of(
                User.builder()
                        .id(providerId)
                        .adminId("admin123")
                        .managedPlaceIds(List.of())
                        .build()
        ));
        when(placeService.getPlaceById(placeId)).thenReturn(place);
        when(serviceService.getServiceById(serviceId)).thenReturn(
                Service.builder().placeId(placeId).build()
        );
        when(queueRepository.save(any(Queue.class))).thenAnswer(inv -> inv.getArgument(0));

        Queue created = queueService.createNewQueue(providerId, "Test", placeId, serviceId,
                20, true, true, 5, false, true);

        assertNotNull(created);
        assertEquals(20, created.getMaxCapacity());
        assertTrue(created.getSupportsGroupToken());
        assertTrue(created.getEmergencySupport());
        assertEquals(5, created.getEmergencyPriorityWeight());
        assertFalse(created.getRequiresEmergencyApproval());
        assertTrue(created.getAutoApproveEmergency());
    }
// ================= GET QUEUES BY PROVIDER =================

    @Test
    void getQueuesByProviderId() {
        List<Queue> queues = List.of(testQueue);
        when(queueRepository.findByProviderId(providerId)).thenReturn(queues);

        List<Queue> result = queueService.getQueuesByProviderId(providerId);

        assertEquals(1, result.size());
        verify(queueRepository).findByProviderId(providerId);
    }

// ================= GET QUEUES BY PLACE =================

    @Test
    void getQueuesByPlaceId() {
        List<Queue> queues = List.of(testQueue);
        when(queueRepository.findByPlaceId(placeId)).thenReturn(queues);

        List<Queue> result = queueService.getQueuesByPlaceId(placeId);

        assertEquals(1, result.size());
        verify(queueRepository).findByPlaceId(placeId);
    }

// ================= GET QUEUES BY SERVICE =================

    @Test
    void getQueuesByServiceId() {
        List<Queue> queues = List.of(testQueue);
        when(queueRepository.findByServiceId(serviceId)).thenReturn(queues);

        List<Queue> result = queueService.getQueuesByServiceId(serviceId);

        assertEquals(1, result.size());
        verify(queueRepository).findByServiceId(serviceId);
    }

// ================= SET QUEUE ACTIVE STATUS =================

    @Test
    void setQueueActiveStatus() {
        testQueue.setIsActive(false);
        when(queueRepository.findById(queueId)).thenReturn(Optional.of(testQueue));
        when(queueRepository.save(any(Queue.class))).thenAnswer(inv -> inv.getArgument(0));

        Queue updated = queueService.setQueueActiveStatus(queueId, true);

        assertTrue(updated.getIsActive());
        verify(messagingTemplate).convertAndSend(eq("/topic/queues/" + queueId), any(Queue.class));
    }

// ================= UPDATE QUEUE STATISTICS =================

    @Test
    void updateQueueStatistics() {
        testQueue.getTokens().add(createTestToken("T-001", TokenStatus.COMPLETED.toString()));
        testQueue.getTokens().add(createTestToken("T-002", TokenStatus.WAITING.toString()));
        testQueue.setStatistics(new Queue.QueueStatistics());

        when(queueRepository.findById(queueId)).thenReturn(Optional.of(testQueue));
        when(queueRepository.save(any(Queue.class))).thenAnswer(inv -> inv.getArgument(0));

        Queue updated = queueService.updateQueueStatistics(queueId);

        assertEquals(1, updated.getStatistics().getTotalServed());
        verify(messagingTemplate).convertAndSend(eq("/topic/queues/" + queueId), any(Queue.class));
    }

// ================= CALCULATE CURRENT WAIT TIME =================

    @Test
    void calculateCurrentWaitTime() {
        for (int i = 0; i < 5; i++) {
            testQueue.getTokens().add(createTestToken("T-00" + i, TokenStatus.WAITING.toString()));
        }
        Service service = new Service();
        service.setAverageServiceTime(3);

        when(queueRepository.findById(queueId)).thenReturn(Optional.of(testQueue));
        when(serviceService.getServiceById(serviceId)).thenReturn(service);

        int waitTime = queueService.calculateCurrentWaitTime(queueId);

        assertEquals(15, waitTime); // 5 * 3
    }

    @Test
    void calculateCurrentWaitTimeNoService() {
        testQueue.getTokens().add(createTestToken("T-001", TokenStatus.WAITING.toString()));

        when(queueRepository.findById(queueId)).thenReturn(Optional.of(testQueue));
        when(serviceService.getServiceById(serviceId)).thenReturn(null);

        int waitTime = queueService.calculateCurrentWaitTime(queueId);

        assertEquals(5, waitTime); // default 5
    }

// ================= GET BEST TIME TO JOIN =================

    @Test
    void getBestTimeToJoin() {
        LocalDateTime now = LocalDateTime.now();
        List<QueueHourlyStats> stats = List.of(
                createStat(now.withHour(9), 10),
                createStat(now.withHour(10), 5),
                createStat(now.withHour(11), 8)
        );
        when(statsRepository.findByQueueIdAndHourBetween(eq(queueId), any(), any())).thenReturn(stats);

        Map<String, Object> result = queueService.getBestTimeToJoin(queueId);

        assertTrue(result.containsKey("bestHours"));
        assertTrue(result.containsKey("averageWaitTimes"));
        List<String> bestHours = (List<String>) result.get("bestHours");
        assertEquals(3, bestHours.size());
        assertEquals("10:00 - 11:00", bestHours.get(0)); // lowest average (5)
    }

    private QueueHourlyStats createStat(LocalDateTime hour, int count) {
        QueueHourlyStats stat = new QueueHourlyStats();
        stat.setHour(hour);
        stat.setWaitingCount(count);
        return stat;
    }

    @Test
    void getUserPosition_UserInWaitingQueue_ReturnsCorrectPosition() {
        // Arrange
        QueueToken token1 = createTestToken("T-001", TokenStatus.WAITING.toString());
        QueueToken token2 = createTestToken("T-002", TokenStatus.WAITING.toString());
        token2.setUserId("user2");
        QueueToken token3 = createTestToken("T-003", TokenStatus.WAITING.toString());
        token3.setUserId("user3");

        testQueue.getTokens().addAll(List.of(token1, token2, token3));

        when(queueRepository.findById(queueId)).thenReturn(Optional.of(testQueue));

        // Act
        UserPositionDTO position = queueService.getUserPosition(queueId, userId);

        // Assert
        assertEquals(1, position.getPosition());
        assertEquals("T-001", position.getTokenId());
    }

    @Test
    void getUserPosition_UserNotInQueue_ReturnsNullPosition() {
        when(queueRepository.findById(queueId)).thenReturn(Optional.of(testQueue));

        UserPositionDTO position = queueService.getUserPosition(queueId, "nonExistentUser");

        assertNull(position.getPosition());
        assertNull(position.getTokenId());
    }
}