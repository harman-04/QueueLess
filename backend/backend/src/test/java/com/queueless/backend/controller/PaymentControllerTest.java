package com.queueless.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.queueless.backend.dto.OrderResponse;
import com.queueless.backend.dto.TokenResponse;
import com.queueless.backend.enums.Role;
import com.queueless.backend.model.Token;
import com.queueless.backend.service.PaymentService;
import com.razorpay.Order;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.cache.type=none",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration"
})
@AutoConfigureMockMvc
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @Autowired
    private ObjectMapper objectMapper;

    private final String orderId = "order_test123";
    private final String paymentId = "pay_test123";
    private final String email = "test@example.com";
    private final String providerEmail = "provider@example.com";
    private final String tokenType = "1_MONTH";
    private final String adminId = "admin123";
    private final String tokenValue = "ADMIN-abc123";

    // ==================== CREATE ORDER ====================

    @Test
    void createOrder_Success() throws Exception {
        Order mockOrder = mockOrder(orderId, 10000);
        when(paymentService.createOrder(eq(email), eq(Role.ADMIN), eq(tokenType), isNull()))
                .thenReturn(mockOrder);

        mockMvc.perform(post("/api/payment/create-order")
                        .param("email", email)
                        .param("role", Role.ADMIN.name())
                        .param("tokenType", tokenType))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.amount").value(10000))
                .andExpect(jsonPath("$.currency").value("INR"));
    }

    @Test
    void createOrder_InvalidTokenType() throws Exception {
        when(paymentService.createOrder(eq(email), eq(Role.ADMIN), eq("INVALID"), isNull()))
                .thenThrow(new IllegalArgumentException("Invalid token type"));

        mockMvc.perform(post("/api/payment/create-order")
                        .param("email", email)
                        .param("role", Role.ADMIN.name())
                        .param("tokenType", "INVALID"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrder_RazorpayException() throws Exception {
        when(paymentService.createOrder(eq(email), eq(Role.ADMIN), eq(tokenType), isNull()))
                .thenThrow(new RazorpayException("Razorpay error"));

        mockMvc.perform(post("/api/payment/create-order")
                        .param("email", email)
                        .param("role", Role.ADMIN.name())
                        .param("tokenType", tokenType))
                .andExpect(status().isInternalServerError());
    }

    // ==================== CONFIRM PAYMENT ====================

    @Test
    void confirmPayment_Success() throws Exception {
        Token token = Token.builder()
                .tokenValue(tokenValue)
                .build();
        when(paymentService.generateToken(email, tokenType, Role.ADMIN, false, null))
                .thenReturn(token);

        mockMvc.perform(post("/api/payment/confirm")
                        .param("orderId", orderId)
                        .param("paymentId", paymentId)
                        .param("email", email)
                        .param("tokenType", tokenType))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenValue").value(tokenValue));
    }

    @Test
    void confirmPayment_PaymentNotFound() throws Exception {
        doThrow(new RuntimeException("Payment not found"))
                .when(paymentService).confirmPayment(orderId, paymentId);

        mockMvc.perform(post("/api/payment/confirm")
                        .param("orderId", orderId)
                        .param("paymentId", paymentId)
                        .param("email", email)
                        .param("tokenType", tokenType))
                .andExpect(status().isBadRequest()); // 400, not 404
    }

    // ==================== CONFIRM PROVIDER PAYMENT ====================

    @Test
    void confirmProviderPayment_Success() throws Exception {
        Token token = Token.builder()
                .tokenValue("PROVIDER-xyz789")
                .build();
        when(paymentService.getAdminIdFromOrder(orderId)).thenReturn(adminId);
        when(paymentService.generateToken(providerEmail, tokenType, Role.PROVIDER, true, adminId))
                .thenReturn(token);

        mockMvc.perform(post("/api/payment/confirm-provider")
                        .param("orderId", orderId)
                        .param("paymentId", paymentId)
                        .param("providerEmail", providerEmail)
                        .param("tokenType", tokenType))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenValue").value("PROVIDER-xyz789"));
    }

    @Test
    void confirmProviderPayment_PaymentNotFound() throws Exception {
        doThrow(new RuntimeException("Payment not found"))
                .when(paymentService).confirmPayment(orderId, paymentId);

        mockMvc.perform(post("/api/payment/confirm-provider")
                        .param("orderId", orderId)
                        .param("paymentId", paymentId)
                        .param("providerEmail", providerEmail)
                        .param("tokenType", tokenType))
                .andExpect(status().isBadRequest()); // 400, not 404
    }

    @Test
    void confirmProviderPayment_AdminIdExtractionError() throws Exception {
        when(paymentService.getAdminIdFromOrder(orderId))
                .thenThrow(new RuntimeException("Failed to retrieve admin ID"));

        mockMvc.perform(post("/api/payment/confirm-provider")
                        .param("orderId", orderId)
                        .param("paymentId", paymentId)
                        .param("providerEmail", providerEmail)
                        .param("tokenType", tokenType))
                .andExpect(status().isBadRequest()); // 400, not 500
    }

    // ==================== BULK CONFIRM PROVIDER PAYMENTS ====================

    @Test
    @WithMockUser(username = adminId)
    void confirmProviderPaymentBulk_Success() throws Exception {
        List<String> emails = List.of("prov1@test.com", "prov2@test.com");
        List<String> tokenValues = List.of("PROVIDER-1", "PROVIDER-2");

        Token token1 = Token.builder().tokenValue(tokenValues.get(0)).build();
        Token token2 = Token.builder().tokenValue(tokenValues.get(1)).build();

        when(paymentService.generateToken(emails.get(0), tokenType, Role.PROVIDER, true, adminId))
                .thenReturn(token1);
        when(paymentService.generateToken(emails.get(1), tokenType, Role.PROVIDER, true, adminId))
                .thenReturn(token2);

        mockMvc.perform(post("/api/payment/confirm-provider-bulk")
                        .param("orderId", orderId)
                        .param("paymentId", paymentId)
                        .param("tokenType", tokenType)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(emails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tokenValue").value(tokenValues.get(0)))
                .andExpect(jsonPath("$[1].tokenValue").value(tokenValues.get(1)));
    }

    @Test
    @WithMockUser(username = adminId)
    void confirmProviderPaymentBulk_PaymentNotFound() throws Exception {
        List<String> emails = List.of("prov1@test.com");

        doThrow(new RuntimeException("Payment not found"))
                .when(paymentService).confirmPayment(orderId, paymentId);

        mockMvc.perform(post("/api/payment/confirm-provider-bulk")
                        .param("orderId", orderId)
                        .param("paymentId", paymentId)
                        .param("tokenType", tokenType)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(emails)))
                .andExpect(status().isBadRequest()); // 400, not 404
    }

    // Helper to create a mock Razorpay Order
    private Order mockOrder(String id, int amount) {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("amount", amount);
        json.put("currency", "INR");
        return new Order(json);
    }
}