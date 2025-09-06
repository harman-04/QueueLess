// File: src/main/java/com/queueless/backend/controller/AdminController.java

package com.queueless.backend.controller;

import com.queueless.backend.model.Payment;
import com.queueless.backend.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final PaymentRepository paymentRepository;

    @GetMapping("/payments")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Payment> getMyPaymentHistory() {
        log.info("Fetching my payment history for admin dashboard.");

        // Get the authenticated user's email from the security context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String adminEmail = authentication.getName(); // The name is typically the email in your setup

        try {
            // Find payments created for this specific admin's email
            List<Payment> payments = paymentRepository.findByCreatedForEmail(adminEmail);
            log.info("Found {} payments for admin with email: {}", payments.size(), adminEmail);
            return payments;
        } catch (Exception e) {
            log.error("Failed to fetch payment history for email: {}", adminEmail, e);
            throw e;
        }
    }
}