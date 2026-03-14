package com.queueless.backend.service;

import com.queueless.backend.dto.AdminQueueDTO;
import com.queueless.backend.dto.ForgotPasswordRequest;
import com.queueless.backend.dto.ProviderDetailsDTO;
import com.queueless.backend.dto.ProviderUpdateRequest;
import com.queueless.backend.exception.AccessDeniedException;
import com.queueless.backend.exception.ResourceNotFoundException;
import com.queueless.backend.model.*;
import com.queueless.backend.model.Queue;
import com.queueless.backend.repository.*;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private QueueRepository queueRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private FeedbackRepository feedbackRepository;

    @Mock
    private PlaceService placeService;

    @Mock
    private PasswordResetService passwordResetService;

    @InjectMocks
    private AdminService adminService;



    private final String adminId = "admin123";
    private final String placeId = "place123";
    private final String providerId = "provider123";
    private final String queueId = "queue123";

    private Place testPlace;
    private User testProvider;
    private Queue testQueue;
    private Payment testPayment;

    @BeforeEach
    void setUp() {
        testPlace = new Place();
        testPlace.setId(placeId);
        testPlace.setName("Test Place");
        testPlace.setAdminId(adminId);

        testProvider = User.builder()
                .id(providerId)
                .name("Test Provider")
                .email("provider@test.com")
                .role(com.queueless.backend.enums.Role.PROVIDER)
                .adminId(adminId)
                .managedPlaceIds(List.of(placeId))
                .build();

        testQueue = new Queue(providerId, "Test Service", placeId, "service123");
        testQueue.setId(queueId);
        testQueue.setIsActive(true);
        testQueue.setTokens(new ArrayList<>());

        testPayment = Payment.builder()
                .id("pay123")
                .razorpayOrderId("order123")
                .amount(10000)
                .createdForEmail("admin@test.com")
                .role(com.queueless.backend.enums.Role.ADMIN)
                .isPaid(true)
                .createdAt(LocalDateTime.now())
                .createdByAdminId(adminId)
                .build();
    }

    // ================= DASHBOARD STATS =================

    @Test
    void getDashboardStatsSuccess() {
        List<Place> adminPlaces = List.of(testPlace);
        when(placeRepository.findByAdminId(adminId)).thenReturn(adminPlaces);

        List<String> placeIds = List.of(placeId);
        List<Queue> queues = List.of(testQueue);
        when(queueRepository.findByPlaceIdIn(placeIds)).thenReturn(queues);

        // Mock users to include one provider under this admin
        List<User> allUsers = List.of(testProvider);
        when(userRepository.findAll()).thenReturn(allUsers);

        Map<String, Object> stats = adminService.getDashboardStats(adminId);

        assertNotNull(stats);
        assertEquals(1, stats.get("totalPlaces"));
        assertEquals(1, stats.get("totalQueues"));
        assertEquals(1L, stats.get("activeQueues"));
        assertEquals(0L, stats.get("tokensServedToday"));
        assertEquals(0L, stats.get("activeUsers"));
        assertEquals(1L, stats.get("providerCount"));
        assertNotNull(stats.get("recentActivity"));
    }

    // ================= PROVIDERS WITH QUEUES =================

    @Test
    void getProvidersWithQueuesSuccess() {
        // 1. Setup Mock Data
        List<User> providers = List.of(testProvider);
        when(userRepository.findAll()).thenReturn(providers);

        // The service calls findByProviderId multiple times (once for main list, once for cancellation rate)
        when(queueRepository.findByProviderId(providerId)).thenReturn(List.of(testQueue));

        // Mock feedback to prevent NullPointerException during getAverageRatingForProvider
        when(feedbackRepository.findByProviderId(providerId)).thenReturn(Collections.emptyList());

        // 2. Call the Service
        List<com.queueless.backend.dto.ProviderPerformanceDTO> result = adminService.getProvidersWithQueues(adminId);

        // 3. Assertions
        assertEquals(1, result.size());
        com.queueless.backend.dto.ProviderPerformanceDTO dto = result.get(0);

        assertEquals(testProvider.getId(), dto.getId());
        assertEquals(testProvider.getName(), dto.getName());
        assertEquals(1, dto.getTotalQueues());
        assertEquals(1, dto.getActiveQueues()); // testQueue.setIsActive(true) was set in @BeforeEach
        assertEquals(0.0, dto.getAverageRating());
    }
    // ================= ADMIN QUEUES WITH DETAILS =================

    @Test
    void getAdminQueuesWithDetailsSuccess() {
        List<Place> adminPlaces = List.of(testPlace);
        when(placeRepository.findByAdminId(adminId)).thenReturn(adminPlaces);
        when(queueRepository.findByPlaceIdIn(List.of(placeId))).thenReturn(List.of(testQueue));
        when(userRepository.findAllById(List.of(providerId))).thenReturn(List.of(testProvider));

        List<AdminQueueDTO> dtos = adminService.getAdminQueuesWithDetails(adminId);

        assertEquals(1, dtos.size());
        AdminQueueDTO dto = dtos.get(0);
        assertEquals(queueId, dto.getId());
        assertEquals(testQueue.getServiceName(), dto.getServiceName());
        assertEquals(testPlace.getName(), dto.getPlaceName());
        assertEquals(testProvider.getName(), dto.getProviderName());
        assertTrue(dto.getIsActive());
        assertEquals(0, dto.getWaitingTokens());
        assertEquals(0, dto.getInServiceTokens());
        assertEquals(0, dto.getCompletedTokens());
        assertEquals(testQueue.getEstimatedWaitTime(), dto.getEstimatedWaitTime());
    }

    // ================= ADMIN PAYMENT HISTORY =================

    @Test
    void getAdminPaymentHistorySuccess() {
        User adminUser = User.builder()
                .id(adminId)
                .email("admin@test.com")
                .build();
        when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));

        List<Payment> providerPayments = List.of(testPayment);
        when(paymentRepository.findByCreatedByAdminId(adminId)).thenReturn(providerPayments);

        List<Payment> adminPayments = List.of(testPayment);
        when(paymentRepository.findByCreatedForEmail("admin@test.com")).thenReturn(adminPayments);

        List<Payment> result = adminService.getAdminPaymentHistory(adminId);

        assertEquals(2, result.size());
        verify(paymentRepository).findByCreatedByAdminId(adminId);
        verify(paymentRepository).findByCreatedForEmail("admin@test.com");
    }

    @Test
    void getAdminPaymentHistoryAdminNotFound() {
        when(userRepository.findById(adminId)).thenReturn(Optional.empty());

        List<Payment> result = adminService.getAdminPaymentHistory(adminId);

        assertTrue(result.isEmpty());
        verify(paymentRepository, never()).findByCreatedByAdminId(any());
        verify(paymentRepository, never()).findByCreatedForEmail(any());
    }

    @Test
    void getAdminReportSuccess() {
        // 1. Setup Mock Data
        User admin = User.builder()
                .id(adminId)
                .name("Admin Name")
                .email("admin@test.com")
                .build();

        // Create a completed token to test wait time and served counts
        QueueToken completedToken = new QueueToken();
        completedToken.setStatus("COMPLETED");
        completedToken.setIssuedAt(LocalDateTime.now().minusMinutes(30));
        completedToken.setServedAt(LocalDateTime.now().minusMinutes(10)); // 20 min wait
        completedToken.setCompletedAt(LocalDateTime.now().minusMinutes(5));

        testQueue.setTokens(List.of(completedToken));

        // Create feedback
        Feedback feedback = new Feedback();
        feedback.setRating( 4);

        // 2. Mocking Repository Calls
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(placeRepository.findByAdminId(adminId)).thenReturn(List.of(testPlace));
        when(queueRepository.findByPlaceId(placeId)).thenReturn(List.of(testQueue));
        when(feedbackRepository.findByPlaceId(placeId)).thenReturn(List.of(feedback));

        // 3. Execute
        com.queueless.backend.dto.AdminReportDTO report = adminService.getAdminReport(adminId);

        // 4. Assertions
        assertNotNull(report);
        assertEquals("Admin Name", report.getAdminName());

        // Global Summary Assertions
        assertEquals(1, report.getSummary().getTotalPlaces());
        assertEquals(1, report.getSummary().getTotalTokensServedToday());
        assertEquals(4, report.getSummary().getAverageRatingOverall());
        assertEquals(20.0, report.getSummary().getAverageWaitTimeOverall()); // 30 - 10 = 20

        // Place Specific Assertions
        assertEquals(1, report.getPlaces().size());
        var placeSummary = report.getPlaces().get(0);
        assertEquals("Test Place", placeSummary.getPlaceName());
        assertEquals(1, placeSummary.getTotalQueues());
    }

    @Test
    void getProviderById_Success() {
        String adminId = "admin123";
        String providerId = "provider123";

        User provider = User.builder()
                .id(providerId)
                .name("Test Provider")
                .email("provider@test.com")
                .adminId(adminId)
                .managedPlaceIds(List.of("place1", "place2"))
                .isActive(true)
                .build();

        when(userRepository.findById(providerId)).thenReturn(Optional.of(provider));

        List<Queue> queues = List.of(testQueue); // testQueue from @BeforeEach
        when(queueRepository.findByProviderId(providerId)).thenReturn(queues);

        // Mock feedback for rating and cancellation rate
        when(feedbackRepository.findByProviderId(providerId)).thenReturn(List.of());

        // Mock places
        Place place1 = new Place(); place1.setId("place1"); place1.setName("Place 1");
        Place place2 = new Place(); place2.setId("place2"); place2.setName("Place 2");
        when(placeService.getPlacesByIds(List.of("place1", "place2"))).thenReturn(List.of(place1, place2));

        ProviderDetailsDTO result = adminService.getProviderById(providerId, adminId);

        assertNotNull(result);
        assertEquals(providerId, result.getId());
        assertEquals("Test Provider", result.getName());
        assertEquals(2, result.getManagedPlaces().size());
        assertEquals(1, result.getTotalQueues()); // from testQueue
        // other assertions as needed
    }

    @Test
    void getProviderById_NotFound() {
        String adminId = "admin123";
        String providerId = "provider123";

        when(userRepository.findById(providerId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> adminService.getProviderById(providerId, adminId));
    }

    @Test
    void getProviderById_AccessDenied() {
        String adminId = "admin123";
        String providerId = "provider123";
        String otherAdmin = "otherAdmin";

        User provider = User.builder()
                .id(providerId)
                .adminId(otherAdmin) // different admin
                .build();

        when(userRepository.findById(providerId)).thenReturn(Optional.of(provider));

        assertThrows(AccessDeniedException.class,
                () -> adminService.getProviderById(providerId, adminId));
    }

    @Test
    void updateProvider_Success() {
        String adminId = "admin123";
        String providerId = "provider123";

        User provider = User.builder()
                .id(providerId)
                .name("Old Name")
                .email("old@test.com")
                .adminId(adminId)
                .managedPlaceIds(List.of("place1"))
                .build();

        ProviderUpdateRequest request = new ProviderUpdateRequest();
        request.setName("New Name");
        request.setEmail("new@test.com");
        request.setPhoneNumber("9876543210");
        request.setManagedPlaceIds(List.of("place2", "place3"));
        request.setIsActive(false);

        when(userRepository.findById(providerId)).thenReturn(Optional.of(provider));

        // Mock place validation
        Place place2 = new Place(); place2.setId("place2");
        Place place3 = new Place(); place3.setId("place3");
        when(placeRepository.findByAdminId(adminId)).thenReturn(List.of(place2, place3));

        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        // Mock the subsequent getProviderById call (or we can capture and assert directly)
        // For simplicity, we'll just call update and verify the saved provider.
        // But updateProvider calls getProviderById at the end, which needs mocks.
        // We can either mock getProviderById or let it execute with proper mocks.

        // To avoid complex mocking, we'll verify the saved user and then manually create expected DTO.
        // However, updateProvider returns a DTO from getProviderById, which requires queue/feedback mocks.
        // For unit test, we can mock getProviderById after save to return a dummy DTO.

        // Let's mock the repository calls and then use spy to partially mock? Simpler: we'll let getProviderById run with minimal mocks.
        // We'll mock queueRepository, feedbackRepository, placeService as needed.

        // For now, we'll just verify that the provider was saved with updated fields, and then expect getProviderById to be called.
        // We'll mock the subsequent call to return a dummy DTO.

        when(placeService.getPlacesByIds(anyList())).thenReturn(List.of());
        when(queueRepository.findByProviderId(providerId)).thenReturn(List.of());
        when(feedbackRepository.findByProviderId(providerId)).thenReturn(List.of());

        ProviderDetailsDTO result = adminService.updateProvider(providerId, request, adminId);

        // Verify provider fields were updated
        assertEquals("New Name", provider.getName());
        assertEquals("new@test.com", provider.getEmail());
        assertEquals("9876543210", provider.getPhoneNumber());
        assertEquals(List.of("place2", "place3"), provider.getManagedPlaceIds());
        assertFalse(provider.getIsActive());

        // Verify result (from getProviderById) is not null
        assertNotNull(result);
    }

    @Test
    void updateProvider_InvalidPlaceId() {
        String adminId = "admin123";
        String providerId = "provider123";

        User provider = User.builder()
                .id(providerId)
                .adminId(adminId)
                .build();

        ProviderUpdateRequest request = new ProviderUpdateRequest();
        request.setManagedPlaceIds(List.of("invalidPlace"));

        when(userRepository.findById(providerId)).thenReturn(Optional.of(provider));
        when(placeRepository.findByAdminId(adminId)).thenReturn(List.of()); // no valid places

        assertThrows(IllegalArgumentException.class,
                () -> adminService.updateProvider(providerId, request, adminId));
    }

    @Test
    void updateProvider_AccessDenied() {
        String adminId = "admin123";
        String providerId = "provider123";

        User provider = User.builder()
                .id(providerId)
                .adminId("otherAdmin")
                .build();

        when(userRepository.findById(providerId)).thenReturn(Optional.of(provider));

        assertThrows(AccessDeniedException.class,
                () -> adminService.updateProvider(providerId, new ProviderUpdateRequest(), adminId));
    }

    @Test
    void toggleProviderStatus_Success() {
        String adminId = "admin123";
        String providerId = "provider123";

        User provider = User.builder()
                .id(providerId)
                .adminId(adminId)
                .isActive(true)
                .build();

        when(userRepository.findById(providerId)).thenReturn(Optional.of(provider));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        // Mock dependencies for getProviderById call
        when(placeService.getPlacesByIds(anyList())).thenReturn(List.of());
        when(queueRepository.findByProviderId(providerId)).thenReturn(List.of());
        when(feedbackRepository.findByProviderId(providerId)).thenReturn(List.of());

        ProviderDetailsDTO result = adminService.toggleProviderStatus(providerId, false, adminId);

        assertFalse(provider.getIsActive());
        verify(userRepository).save(provider);
        assertNotNull(result);
    }

    @Test
    void toggleProviderStatus_NotFound() {
        String adminId = "admin123";
        String providerId = "provider123";

        when(userRepository.findById(providerId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> adminService.toggleProviderStatus(providerId, false, adminId));
    }

    @Test
    void toggleProviderStatus_AccessDenied() {
        String adminId = "admin123";
        String providerId = "provider123";

        User provider = User.builder()
                .id(providerId)
                .adminId("otherAdmin")
                .build();

        when(userRepository.findById(providerId)).thenReturn(Optional.of(provider));

        assertThrows(AccessDeniedException.class,
                () -> adminService.toggleProviderStatus(providerId, false, adminId));
    }

    @Test
    void resetProviderPassword_Success() throws MessagingException {
        String adminId = "admin123";
        String providerId = "provider123";
        String email = "provider@test.com";

        User provider = User.builder()
                .id(providerId)
                .adminId(adminId)
                .email(email)
                .build();

        when(userRepository.findById(providerId)).thenReturn(Optional.of(provider));
        when(passwordResetService.sendOtp(any(ForgotPasswordRequest.class))).thenReturn("OTP sent");

        adminService.resetProviderPassword(providerId, adminId);

        verify(passwordResetService).sendOtp(argThat(req -> req.getEmail().equals(email)));
    }
    @Test
    void resetProviderPassword_NotFound() {
        String adminId = "admin123";
        String providerId = "provider123";

        when(userRepository.findById(providerId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> adminService.resetProviderPassword(providerId, adminId));
    }

    @Test
    void resetProviderPassword_AccessDenied() {
        String adminId = "admin123";
        String providerId = "provider123";

        User provider = User.builder()
                .id(providerId)
                .adminId("otherAdmin")
                .build();

        when(userRepository.findById(providerId)).thenReturn(Optional.of(provider));

        assertThrows(AccessDeniedException.class,
                () -> adminService.resetProviderPassword(providerId, adminId));
    }
}