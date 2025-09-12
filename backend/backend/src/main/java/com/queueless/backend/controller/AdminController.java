// Updated AdminController.java
package com.queueless.backend.controller;

import com.queueless.backend.dto.AdminQueueDTO;
import com.queueless.backend.model.Payment;
import com.queueless.backend.model.Place;
import com.queueless.backend.model.Queue;
import com.queueless.backend.model.User;
import com.queueless.backend.repository.PaymentRepository;
import com.queueless.backend.repository.PlaceRepository;
import com.queueless.backend.repository.QueueRepository;
import com.queueless.backend.repository.UserRepository;
import com.queueless.backend.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final PaymentRepository paymentRepository;
    private final AdminService adminService;
    private final UserRepository userRepository;
    private final PlaceRepository placeRepository;
    private final QueueRepository queueRepository;

    @GetMapping("/payments")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Payment> getMyPaymentHistory() {
        log.info("Fetching payment history for admin dashboard.");

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String adminId = authentication.getName();

        try {
            // Get admin user to retrieve email
            Optional<User> adminUser = userRepository.findById(adminId);
            if (adminUser.isEmpty()) {
                log.error("Admin user not found with ID: {}", adminId);
                return List.of();
            }

            String adminEmail = adminUser.get().getEmail();
            List<Payment> payments = paymentRepository.findByCreatedForEmail(adminEmail);
            log.info("Found {} payments for admin with email: {}", payments.size(), adminEmail);
            return payments;
        } catch (Exception e) {
            log.error("Failed to fetch payment history for admin ID: {}", adminId, e);
            throw e;
        }
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> getAdminStats() {
        log.info("Fetching admin dashboard statistics");

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String adminId = authentication.getName();

        try {
            return adminService.getDashboardStats(adminId);
        } catch (Exception e) {
            log.error("Failed to fetch admin stats for adminId: {}", adminId, e);
            throw new RuntimeException("Failed to fetch dashboard statistics");
        }
    }


    // Add these endpoints to AdminController.java

    @GetMapping("/providers")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Map<String, Object>> getProvidersWithQueues() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String adminId = authentication.getName();

        try {
            return adminService.getProvidersWithQueues(adminId);
        } catch (Exception e) {
            log.error("Failed to fetch providers with queues for adminId: {}", adminId, e);
            throw new RuntimeException("Failed to fetch providers data");
        }
    }

    @GetMapping("/queues")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Queue> getAllAdminQueues() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String adminId = authentication.getName();

        try {
            // Get places owned by this admin
            List<Place> adminPlaces = placeRepository.findByAdminId(adminId);
            List<String> placeIds = adminPlaces.stream().map(Place::getId).toList();

            // Get all queues for these places
            return queueRepository.findByPlaceIdIn(placeIds);
        } catch (Exception e) {
            log.error("Failed to fetch queues for adminId: {}", adminId, e);
            throw new RuntimeException("Failed to fetch queues");
        }
    }


    // Add new endpoints to AdminController.java
    @GetMapping("/queues/enhanced")
    @PreAuthorize("hasRole('ADMIN')")
    public List<AdminQueueDTO> getAdminQueuesWithDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String adminId = authentication.getName();

        try {
            return adminService.getAdminQueuesWithDetails(adminId);
        } catch (Exception e) {
            log.error("Failed to fetch enhanced queues for adminId: {}", adminId, e);
            throw new RuntimeException("Failed to fetch queues with details");
        }
    }

    @GetMapping("/payments/enhanced")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Payment> getEnhancedPaymentHistory() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String adminId = authentication.getName();

        try {
            return adminService.getAdminPaymentHistory(adminId);
        } catch (Exception e) {
            log.error("Failed to fetch enhanced payment history for adminId: {}", adminId, e);
            throw new RuntimeException("Failed to fetch payment history");
        }
    }
}