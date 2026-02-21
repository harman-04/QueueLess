package com.queueless.backend.service;

import com.queueless.backend.dto.AdminQueueDTO;
import com.queueless.backend.model.Payment;
import com.queueless.backend.model.Place;
import com.queueless.backend.model.Queue;
import com.queueless.backend.model.User;
import com.queueless.backend.repository.PaymentRepository;
import com.queueless.backend.repository.PlaceRepository;
import com.queueless.backend.repository.QueueRepository;
import com.queueless.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
        List<User> providers = List.of(testProvider);
        when(userRepository.findAll()).thenReturn(providers);
        when(queueRepository.findByProviderId(providerId)).thenReturn(List.of(testQueue));

        List<Map<String, Object>> result = adminService.getProvidersWithQueues(adminId);

        assertEquals(1, result.size());
        Map<String, Object> providerData = result.get(0);
        assertEquals(testProvider, providerData.get("provider"));
        assertEquals(List.of(testQueue), providerData.get("queues"));
        assertNotNull(providerData.get("stats"));
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
}