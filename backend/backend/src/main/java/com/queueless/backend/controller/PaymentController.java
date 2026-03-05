package com.queueless.backend.controller;

import com.queueless.backend.dto.OrderResponse;
import com.queueless.backend.dto.TokenResponse;
import com.queueless.backend.enums.Role;
import com.queueless.backend.model.Token;
import com.queueless.backend.service.PaymentService;
import com.razorpay.Order;
import com.razorpay.RazorpayException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Endpoints for handling payments via Razorpay")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create-order")
    @Operation(summary = "Create a Razorpay order", description = "Creates an order for purchasing an admin or provider token.")
    @ApiResponse(responseCode = "200", description = "Order created",
            content = @Content(schema = @Schema(implementation = OrderResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid token type")
    @ApiResponse(responseCode = "500", description = "Razorpay error")
    public OrderResponse createOrder(
            @Parameter(description = "Email of the user for whom the token is purchased") @RequestParam String email,
            @Parameter(description = "Role (ADMIN or PROVIDER)") @RequestParam Role role,
            @Parameter(description = "Token type: 1_MONTH, 1_YEAR, or LIFETIME") @RequestParam String tokenType,
            @Parameter(description = "Admin ID (required only for PROVIDER tokens)") @RequestParam(required = false) String adminId
    ) throws Exception {
        log.info("createOrder request: email={}, role={}, tokenType={}, adminId={}", email, role, tokenType, adminId);
        Order order = paymentService.createOrder(email, role, tokenType, adminId);
        return new OrderResponse(order.get("id"), order.get("amount"), "INR");
    }

    @PostMapping("/confirm")
    @Operation(summary = "Confirm admin payment", description = "Confirms payment and generates an admin token.")
    @ApiResponse(responseCode = "200", description = "Token generated",
            content = @Content(schema = @Schema(implementation = TokenResponse.class)))
    @ApiResponse(responseCode = "404", description = "Payment not found")
    public TokenResponse confirmPayment(
            @RequestParam String orderId,
            @RequestParam String paymentId,
            @RequestParam String email,
            @RequestParam String tokenType
    ) {
        log.info("confirmPayment: orderId={}, paymentId={}, email={}, tokenType={}", orderId, paymentId, email, tokenType);
        paymentService.confirmPayment(orderId, paymentId);
        Token token = paymentService.generateToken(email, tokenType, Role.ADMIN, false, null);
        return new TokenResponse(token.getTokenValue());
    }

    @PostMapping("/confirm-provider")
    @Operation(summary = "Confirm provider payment", description = "Confirms payment and generates a provider token. Admin ID is extracted from Razorpay order notes.")
    @ApiResponse(responseCode = "200", description = "Token generated",
            content = @Content(schema = @Schema(implementation = TokenResponse.class)))
    @ApiResponse(responseCode = "404", description = "Payment not found")
    @ApiResponse(responseCode = "500", description = "Failed to retrieve admin ID")
    public TokenResponse confirmProviderPayment(
            @RequestParam String orderId,
            @RequestParam String paymentId,
            @RequestParam String providerEmail,
            @RequestParam String tokenType
    ) throws RazorpayException {
        log.info("confirmProviderPayment: orderId={}, paymentId={}, providerEmail={}, tokenType={}", orderId, paymentId, providerEmail, tokenType);
        paymentService.confirmPayment(orderId, paymentId);
        String adminId = paymentService.getAdminIdFromOrder(orderId);
        Token token = paymentService.generateToken(providerEmail, tokenType, Role.PROVIDER, true, adminId);
        return new TokenResponse(token.getTokenValue());
    }

    @PostMapping("/confirm-provider-bulk")
    @Operation(summary = "Bulk confirm provider payments", description = "Confirms a single payment and generates multiple provider tokens for the given emails. Admin ID is taken from authenticated user.")
    @ApiResponse(responseCode = "200", description = "List of generated tokens",
            content = @Content(schema = @Schema(implementation = TokenResponse.class)))
    @ApiResponse(responseCode = "404", description = "Payment not found")
    public List<TokenResponse> confirmProviderPaymentBulk(
            @RequestParam String orderId,
            @RequestParam String paymentId,
            @RequestBody List<String> emails,
            @RequestParam String tokenType,
            Authentication authentication
    ) {
        log.info("confirmProviderPaymentBulk: orderId={}, paymentId={}, emails={}, tokenType={}, adminId={}", orderId, paymentId, emails, tokenType, authentication.getName());
        paymentService.confirmPayment(orderId, paymentId);
        String adminId = authentication.getName();

        return emails.stream()
                .map(email -> paymentService.generateToken(email, tokenType, Role.PROVIDER, true, adminId))
                .map(token -> new TokenResponse(token.getTokenValue()))
                .collect(Collectors.toList());
    }
}