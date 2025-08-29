package com.queueless.backend.service;

import com.queueless.backend.model.Payment;
import com.queueless.backend.model.Role;
import com.queueless.backend.model.Token;
import com.queueless.backend.repository.PaymentRepository;
import com.queueless.backend.repository.TokenRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final TokenRepository tokenRepository;

    @Value("${razorpay.key}")
    private String RAZORPAY_KEY;

    @Value("${razorpay.secret}")
    private String RAZORPAY_SECRET;

    public Order createOrder(String email, Role role, String tokenType) throws Exception {
        log.info("Initiating order creation | email={}, role={}, tokenType={}", email, role, tokenType);

        RazorpayClient razorpayClient = new RazorpayClient(RAZORPAY_KEY, RAZORPAY_SECRET);

        int amount;
        try {
            if (role == Role.ADMIN) {
                switch (tokenType) {
                    case "1_MONTH":
                        amount = 10000;
                        break;
                    case "1_YEAR":
                        amount = 50000;
                        break;
                    case "LIFETIME":
                        amount = 100000;
                        break;
                    default:
                        log.error("Invalid token type provided for ADMIN | tokenType={}", tokenType);
                        throw new IllegalArgumentException("Invalid token type");
                }
            } else if (role == Role.PROVIDER) {
                switch (tokenType) {
                    case "1_MONTH":
                        amount = 10000;
                        break;
                    case "1_YEAR":
                        amount = 50000;
                        break;
                    case "LIFETIME":
                        amount = 100000;
                        break;
                    default:
                        log.error("Invalid token type provided for PROVIDER | tokenType={}", tokenType);
                        throw new IllegalArgumentException("Invalid token type");
                }
            } else {
                log.warn("Unknown role encountered | role={} | assigning default amount", role);
                amount = 20000;
            }

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amount);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "receipt_" + System.currentTimeMillis());

            log.debug("Sending request to Razorpay | payload={}", orderRequest);

            Order order = razorpayClient.orders.create(orderRequest);

            Payment payment = Payment.builder()
                    .razorpayOrderId(order.get("id"))
                    .amount(amount)
                    .createdForEmail(email)
                    .role(role)
                    .isPaid(false)
                    .createdAt(LocalDateTime.now())
                    .build();

            paymentRepository.save(payment);

            log.info("Order created successfully | orderId={}, email={}, role={}, amount={}", order.get("id"), email, role, amount);
            return order;

        } catch (Exception e) {
            log.error("Failed to create order | email={}, role={}, tokenType={}, error={}", email, role, tokenType, e.getMessage(), e);
            throw e;
        }
    }

    public String confirmPayment(String orderId, String paymentId) {
        log.info("Confirming payment | orderId={}, paymentId={}", orderId, paymentId);

        Payment payment = paymentRepository.findByRazorpayOrderId(orderId)
                .orElseThrow(() -> {
                    log.error("Payment not found for orderId={}", orderId);
                    return new RuntimeException("Payment not found");
                });

        payment.setRazorpayPaymentId(paymentId);
        payment.setPaid(true);
        paymentRepository.save(payment);

        log.info("Payment confirmed successfully | orderId={}, paymentId={}, email={}", orderId, paymentId, payment.getCreatedForEmail());

        return "Payment confirmed";
    }

    public Token generateToken(String email, String tokenType, Role role, boolean isProviderToken) {
        log.info("Generating token | email={}, role={}, tokenType={}, isProviderToken={}", email, role, tokenType, isProviderToken);

        LocalDateTime expiry;
        try {
            switch (tokenType) {
                case "1_MONTH":
                    expiry = LocalDateTime.now().plus(1, ChronoUnit.MONTHS);
                    break;
                case "1_YEAR":
                    expiry = LocalDateTime.now().plus(1, ChronoUnit.YEARS);
                    break;
                case "LIFETIME":
                    expiry = LocalDateTime.now().plus(100, ChronoUnit.YEARS);
                    break;
                default:
                    log.error("Invalid token type for token generation | tokenType={}", tokenType);
                    throw new IllegalArgumentException("Invalid token type");
            }

            String tokenValue = (role == Role.ADMIN ? "ADMIN-" : "PROVIDER-") +
                    UUID.randomUUID().toString().replace("-", "").substring(0, 12);

            Token token = Token.builder()
                    .tokenValue(tokenValue)
                    .role(role)
                    .createdForEmail(email)
                    .providerEmail(isProviderToken ? email : null)
                    .expiryDate(expiry)
                    .isUsed(false)
                    .isProviderToken(isProviderToken)
                    .build();

            tokenRepository.save(token);

            log.info("Token generated successfully | tokenValue={}, role={}, expiry={}", tokenValue, role, expiry);

            return token;

        } catch (Exception e) {
            log.error("Failed to generate token | email={}, role={}, tokenType={}, error={}", email, role, tokenType, e.getMessage(), e);
            throw e;
        }
    }
}
