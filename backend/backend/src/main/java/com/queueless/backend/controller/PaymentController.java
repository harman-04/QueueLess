package com.queueless.backend.controller;

import com.queueless.backend.dto.OrderResponse;
import com.queueless.backend.dto.TokenResponse;
import com.queueless.backend.enums.Role;
import com.queueless.backend.model.Token;
import com.queueless.backend.service.PaymentService;
import com.razorpay.Order;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create-order")
    public OrderResponse createOrder(
            @RequestParam String email,
            @RequestParam Role role,
            @RequestParam String tokenType,
            @RequestParam(required = false) String adminId // Make it optional
    ) throws Exception {
        Order order = paymentService.createOrder(email, role, tokenType, adminId);
        return new OrderResponse(order.get("id"), order.get("amount"), "INR");
    }

    // Corrected PaymentController
    @PostMapping("/confirm")
    public TokenResponse confirmPayment(
            @RequestParam String orderId,
            @RequestParam String paymentId,
            @RequestParam String email,
            @RequestParam String tokenType
    ) {
        paymentService.confirmPayment(orderId, paymentId);
        // When generating a token for an admin, there is no "creating admin."
        // `adminId` should be null.
        Token token = paymentService.generateToken(email, tokenType, Role.ADMIN, false, null);
        return new TokenResponse(token.getTokenValue());
    }

    @PostMapping("/confirm-provider")
    public TokenResponse confirmProviderPayment(
            @RequestParam String orderId,
            @RequestParam String paymentId,
            @RequestParam String providerEmail, // Use a more descriptive name
            @RequestParam String tokenType
    ) throws RazorpayException {
        // No authentication object here!
        paymentService.confirmPayment(orderId, paymentId);

        // Fetch the order from Razorpay to get the admin ID from notes
        String adminId = paymentService.getAdminIdFromOrder(orderId);

        Token token = paymentService.generateToken(providerEmail, tokenType, Role.PROVIDER, true, adminId);
        return new TokenResponse(token.getTokenValue());
    }

    @PostMapping("/confirm-provider-bulk")
    public List<TokenResponse> confirmProviderPaymentBulk(
            @RequestParam String orderId,
            @RequestParam String paymentId,
            @RequestBody List<String> emails,
            @RequestParam String tokenType,
            Authentication authentication // This will not be null anymore
    ) {
        paymentService.confirmPayment(orderId, paymentId);
        String adminId = authentication.getName(); // Safely get the ID from the authenticated user

        return emails.stream()
                .map(email -> paymentService.generateToken(email, tokenType, Role.PROVIDER, true, adminId))
                .map(token -> new TokenResponse(token.getTokenValue()))
                .collect(Collectors.toList());
    }
}