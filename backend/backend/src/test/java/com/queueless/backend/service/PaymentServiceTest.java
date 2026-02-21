package com.queueless.backend.service;

import com.queueless.backend.enums.Role;
import com.queueless.backend.model.Payment;
import com.queueless.backend.model.Token;
import com.queueless.backend.repository.PaymentRepository;
import com.queueless.backend.repository.TokenRepository;
import com.razorpay.Order;
import com.razorpay.OrderClient;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private TokenRepository tokenRepository;

    @InjectMocks
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        // Inject configuration properties via ReflectionTestUtils
        ReflectionTestUtils.setField(paymentService, "RAZORPAY_KEY", "test_key");
        ReflectionTestUtils.setField(paymentService, "RAZORPAY_SECRET", "test_secret");
    }

    // ================= CREATE ORDER =================

    @Test
    void createOrderForAdminSuccess() throws Exception {
        String email = "admin@test.com";
        Role role = Role.ADMIN;
        String tokenType = "1_MONTH";

        // 1. Setup sub-mocks
        OrderClient mockOrderClient = mock(OrderClient.class);
        Order mockOrder = mock(Order.class);

        // Only stub what the service actually calls: order.get("id")
        when(mockOrder.get("id")).thenReturn("order_test123");

        // The service calls mockOrderClient.create()
        when(mockOrderClient.create(any(JSONObject.class))).thenReturn(mockOrder);

        // 2. Mock construction and inject the sub-mock
        try (MockedConstruction<RazorpayClient> mocked = mockConstruction(RazorpayClient.class,
                (mock, context) -> {
                    mock.orders = mockOrderClient;
                })) {

            // 3. Act
            Order order = paymentService.createOrder(email, role, tokenType, null);

            // 4. Assert
            assertNotNull(order);
            assertEquals("order_test123", order.get("id"));

            // Verify database interaction
            verify(paymentRepository).save(argThat(payment ->
                    payment.getRazorpayOrderId().equals("order_test123") &&
                            payment.getAmount() == 10000 &&
                            payment.getRole() == Role.ADMIN
            ));
        }
    }

    @Test
    void createOrderForProviderSuccess() throws Exception {
        String email = "provider@test.com";
        Role role = Role.PROVIDER;
        String tokenType = "1_YEAR";
        String adminId = "admin123";

        OrderClient mockOrderClient = mock(OrderClient.class);
        Order mockOrder = mock(Order.class);
        when(mockOrder.get("id")).thenReturn("order_provider123");
        when(mockOrderClient.create(any(JSONObject.class))).thenReturn(mockOrder);

        try (MockedConstruction<RazorpayClient> mocked = mockConstruction(RazorpayClient.class,
                (mock, context) -> {
                    mock.orders = mockOrderClient;
                })) {

            Order order = paymentService.createOrder(email, role, tokenType, adminId);

            assertNotNull(order);

            // Verify the specific payload sent to Razorpay
            ArgumentCaptor<JSONObject> captor = ArgumentCaptor.forClass(JSONObject.class);
            verify(mockOrderClient).create(captor.capture());

            JSONObject sentRequest = captor.getValue();
            assertTrue(sentRequest.has("notes"));
            assertEquals(adminId, sentRequest.getJSONObject("notes").getString("adminId"));
        }
    }
    @Test
    void createOrderInvalidTokenType() {
        String email = "admin@test.com";
        Role role = Role.ADMIN;
        String tokenType = "INVALID";

        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> paymentService.createOrder(email, role, tokenType, null));
        assertEquals("Invalid token type", exception.getMessage());
    }

    @Test
    void createOrderRazorpayException() throws Exception {
        OrderClient mockOrderClient = mock(OrderClient.class);
        when(mockOrderClient.create(any(JSONObject.class)))
                .thenThrow(new RazorpayException("Razorpay error"));

        try (MockedConstruction<RazorpayClient> mocked = mockConstruction(RazorpayClient.class,
                (mock, context) -> {
                    mock.orders = mockOrderClient;
                })) {

            assertThrows(RazorpayException.class,
                    () -> paymentService.createOrder("test@test.com", Role.ADMIN, "1_MONTH", null));
        }
    }

    // ================= CONFIRM PAYMENT =================

    @Test
    void confirmPaymentSuccess() {
        String orderId = "order123";
        String paymentId = "pay123";
        Payment payment = Payment.builder()
                .razorpayOrderId(orderId)
                .isPaid(false)
                .build();

        when(paymentRepository.findByRazorpayOrderId(orderId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        String result = paymentService.confirmPayment(orderId, paymentId);

        assertEquals("Payment confirmed", result);
        assertTrue(payment.isPaid());
        assertEquals(paymentId, payment.getRazorpayPaymentId());
        verify(paymentRepository).save(payment);
    }

    @Test
    void confirmPaymentNotFound() {
        String orderId = "order123";
        when(paymentRepository.findByRazorpayOrderId(orderId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> paymentService.confirmPayment(orderId, "pay123"));
        assertEquals("Payment not found", exception.getMessage());
    }

    // ================= GENERATE TOKEN =================

    @Test
    void generateTokenForAdminOneMonth() {
        String email = "admin@test.com";
        String tokenType = "1_MONTH";
        Role role = Role.ADMIN;
        boolean isProviderToken = false;
        String adminId = null;

        when(tokenRepository.save(any(Token.class))).thenAnswer(inv -> inv.getArgument(0));

        Token token = paymentService.generateToken(email, tokenType, role, isProviderToken, adminId);

        assertNotNull(token);
        assertTrue(token.getTokenValue().startsWith("ADMIN-"));
        assertEquals(role, token.getRole());
        assertEquals(email, token.getCreatedForEmail());
        assertFalse(token.isUsed());
        assertFalse(token.isProviderToken());
        assertNull(token.getCreatedByAdminId());
        assertNotNull(token.getExpiryDate());
        // Expiry should be about 1 month from now
        LocalDateTime expectedExpiry = LocalDateTime.now().plusMonths(1);
        assertTrue(token.getExpiryDate().isAfter(expectedExpiry.minusMinutes(1)) &&
                token.getExpiryDate().isBefore(expectedExpiry.plusMinutes(1)));
    }

    @Test
    void generateTokenForProviderOneYearWithAdminId() {
        String email = "provider@test.com";
        String tokenType = "1_YEAR";
        Role role = Role.PROVIDER;
        boolean isProviderToken = true;
        String adminId = "admin123";

        when(tokenRepository.save(any(Token.class))).thenAnswer(inv -> inv.getArgument(0));

        Token token = paymentService.generateToken(email, tokenType, role, isProviderToken, adminId);

        assertNotNull(token);
        assertTrue(token.getTokenValue().startsWith("PROVIDER-"));
        assertEquals(role, token.getRole());
        assertEquals(email, token.getCreatedForEmail());
        assertEquals(email, token.getProviderEmail());
        assertTrue(token.isProviderToken());
        assertEquals(adminId, token.getCreatedByAdminId());
        LocalDateTime expectedExpiry = LocalDateTime.now().plusYears(1);
        assertTrue(token.getExpiryDate().isAfter(expectedExpiry.minusMinutes(1)) &&
                token.getExpiryDate().isBefore(expectedExpiry.plusMinutes(1)));
    }

    @Test
    void generateTokenLifetime() {
        String email = "admin@test.com";
        String tokenType = "LIFETIME";
        Role role = Role.ADMIN;
        boolean isProviderToken = false;
        String adminId = null;

        when(tokenRepository.save(any(Token.class))).thenAnswer(inv -> inv.getArgument(0));

        Token token = paymentService.generateToken(email, tokenType, role, isProviderToken, adminId);

        LocalDateTime expectedExpiry = LocalDateTime.now().plusYears(100);
        assertTrue(token.getExpiryDate().isAfter(expectedExpiry.minusMinutes(1)) &&
                token.getExpiryDate().isBefore(expectedExpiry.plusMinutes(1)));
    }

    @Test
    void generateTokenInvalidType() {
        String email = "admin@test.com";
        String tokenType = "INVALID";
        Role role = Role.ADMIN;

        assertThrows(IllegalArgumentException.class,
                () -> paymentService.generateToken(email, tokenType, role, false, null));
    }

    // ================= GET ADMIN ID FROM ORDER =================

    @Test
    void getAdminIdFromOrderSuccess() throws Exception {
        String orderId = "order123";
        OrderClient mockOrderClient = mock(OrderClient.class);
        Order mockOrder = mock(Order.class);

        JSONObject notes = new JSONObject();
        notes.put("adminId", "admin123");
        when(mockOrder.get("notes")).thenReturn(notes);
        when(mockOrderClient.fetch(orderId)).thenReturn(mockOrder);

        try (MockedConstruction<RazorpayClient> mocked = mockConstruction(RazorpayClient.class,
                (mock, context) -> {
                    mock.orders = mockOrderClient;
                })) {

            String result = paymentService.getAdminIdFromOrder(orderId);
            assertEquals("admin123", result);
        }
    }

    @Test
    void getAdminIdFromOrderException() throws Exception {
        String orderId = "order123";
        OrderClient mockOrderClient = mock(OrderClient.class);
        when(mockOrderClient.fetch(orderId)).thenThrow(new RazorpayException("Fetch failed"));

        try (MockedConstruction<RazorpayClient> mocked = mockConstruction(RazorpayClient.class,
                (mock, context) -> {
                    mock.orders = mockOrderClient;
                })) {

            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> paymentService.getAdminIdFromOrder(orderId));

            // Checking the cause since the service wraps it in a RuntimeException
            assertEquals("Failed to retrieve admin ID from order", exception.getMessage());
        }
    }
}