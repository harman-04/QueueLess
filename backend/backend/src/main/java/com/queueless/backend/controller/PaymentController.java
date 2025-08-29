package com.queueless.backend.controller;

import com.queueless.backend.dto.OrderResponse;
import com.queueless.backend.dto.TokenResponse;
import com.queueless.backend.model.Role;
import com.queueless.backend.model.Token;
import com.queueless.backend.service.PaymentService;
import com.razorpay.Order;
import lombok.RequiredArgsConstructor;
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
            @RequestParam String tokenType
    ) throws Exception {
        Order order = paymentService.createOrder(email, role, tokenType);
        return new OrderResponse(order.get("id"), order.get("amount"), "INR");
    }

    @PostMapping("/confirm")
    public TokenResponse confirmPayment(
            @RequestParam String orderId,
            @RequestParam String paymentId,
            @RequestParam String email,
            @RequestParam String tokenType
    ) {
        paymentService.confirmPayment(orderId, paymentId);
        Token token = paymentService.generateToken(email, tokenType, Role.ADMIN,true);
        return new TokenResponse(token.getTokenValue());
    }

    @PostMapping("/confirm-provider")
    public TokenResponse confirmProviderPayment(
            @RequestParam String orderId,
            @RequestParam String paymentId,
            @RequestParam String email,
            @RequestParam String tokenType
    ) {
        paymentService.confirmPayment(orderId, paymentId);
        Token token = paymentService.generateToken(email, tokenType, Role.PROVIDER, true);
        return new TokenResponse(token.getTokenValue());
    }

    @PostMapping("/confirm-provider-bulk")
    public List<TokenResponse> confirmProviderPaymentBulk(
            @RequestParam String orderId,
            @RequestParam String paymentId,
            @RequestBody List<String> emails,
            @RequestParam String tokenType
    ) {
        paymentService.confirmPayment(orderId, paymentId);

        return emails.stream()
                .map(email -> paymentService.generateToken(email, tokenType, Role.PROVIDER, true))
                .map(token -> new TokenResponse(token.getTokenValue()))
                .collect(Collectors.toList());
    }


}
