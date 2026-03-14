package com.queueless.backend.controller;

import com.itextpdf.text.DocumentException;
import com.queueless.backend.dto.*;
import com.queueless.backend.exception.ResourceNotFoundException;
import com.queueless.backend.model.*;
import com.queueless.backend.repository.PaymentRepository;
import com.queueless.backend.repository.PlaceRepository;
import com.queueless.backend.repository.QueueRepository;
import com.queueless.backend.repository.UserRepository;
import com.queueless.backend.service.AdminService;
import com.queueless.backend.security.annotations.AdminOnly;
import com.queueless.backend.service.AlertConfigService;
import com.queueless.backend.service.ExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Endpoints for admin dashboard and management")
@Validated
public class AdminController {

    private final PaymentRepository paymentRepository;
    private final AdminService adminService;
    private final UserRepository userRepository;
    private final PlaceRepository placeRepository;
    private final QueueRepository queueRepository;
    private final ExportService exportService;
    private final AlertConfigService alertConfigService;

    @GetMapping("/payments")
    @AdminOnly
    @Operation(summary = "Get my payment history", description = "Returns payment history for the authenticated admin (payments made by them).")
    @ApiResponse(responseCode = "200", description = "List of payments",
            content = @Content(schema = @Schema(implementation = Payment.class)))
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public List<Payment> getMyPaymentHistory() {
        log.info("Fetching payment history for admin dashboard.");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String adminId = authentication.getName();

        try {
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
    @AdminOnly
    @Operation(summary = "Get admin dashboard statistics", description = "Returns aggregated statistics for the admin's dashboard.")
    @ApiResponse(responseCode = "200", description = "Statistics map")
    @ApiResponse(responseCode = "500", description = "Internal server error")
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

    @GetMapping("/queues")
    @AdminOnly
    @Operation(summary = "Get all queues under admin's places", description = "Returns a list of all queues belonging to places owned by the admin.")
    @ApiResponse(responseCode = "200", description = "List of queues")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public List<Queue> getAllAdminQueues() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String adminId = authentication.getName();

        try {
            List<Place> adminPlaces = placeRepository.findByAdminId(adminId);
            List<String> placeIds = adminPlaces.stream().map(Place::getId).toList();

            return queueRepository.findByPlaceIdIn(placeIds);
        } catch (Exception e) {
            log.error("Failed to fetch queues for adminId: {}", adminId, e);
            throw new RuntimeException("Failed to fetch queues");
        }
    }

    @GetMapping("/queues/enhanced")
    @AdminOnly
    @Operation(summary = "Get enhanced queue details", description = "Returns queues with place and provider names, token counts, and estimated wait times.")
    @ApiResponse(responseCode = "200", description = "List of enhanced queue DTOs",
            content = @Content(schema = @Schema(implementation = AdminQueueDTO.class)))
    @ApiResponse(responseCode = "500", description = "Internal server error")
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
    @AdminOnly
    @Operation(summary = "Get enhanced payment history", description = "Returns all payments made by admin (for themselves) and for providers they created, sorted by date.")
    @ApiResponse(responseCode = "200", description = "List of payments")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public ResponseEntity<List<PaymentHistoryDTO>> getEnhancedPaymentHistory() {
        String adminId = SecurityContextHolder.getContext().getAuthentication().getName();
        List<PaymentHistoryDTO> history = adminService.getEnhancedPaymentHistory(adminId);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/analytics/tokens-over-time")
    @AdminOnly
    @Operation(summary = "Get token volume over time", description = "Returns daily token counts for the last N days (default 30).")
    @ApiResponse(responseCode = "200", description = "Map with dates and counts")
    public ResponseEntity<Map<String, Object>> getTokensOverTime(
            @RequestParam(defaultValue = "30") @Min(1) @Max(365) int days) {
        String adminId = SecurityContextHolder.getContext().getAuthentication().getName();
        Map<String, Object> data = adminService.getTokensOverTime(adminId, days);
        return ResponseEntity.ok(data);
    }

    @GetMapping("/analytics/busiest-hours")
    @AdminOnly
    @Operation(summary = "Get busiest hours", description = "Returns average queue waiting count by hour (0-23) over the last 30 days.")
    @ApiResponse(responseCode = "200", description = "Map of hour -> average waiting count")
    public ResponseEntity<Map<Integer, Double>> getBusiestHours() {
        String adminId = SecurityContextHolder.getContext().getAuthentication().getName();
        Map<Integer, Double> data = adminService.getBusiestHours(adminId);
        return ResponseEntity.ok(data);
    }

    @GetMapping("/providers")
    @AdminOnly
    public ResponseEntity<List<ProviderPerformanceDTO>> getProvidersWithQueues() {
        String adminId = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(adminService.getProvidersWithQueues(adminId));
    }

    @GetMapping("/places-with-queues")
    @AdminOnly
    @Operation(summary = "Get places with current queue counts", description = "Returns all places owned by the admin with waiting and in-service token counts.")
    @ApiResponse(responseCode = "200", description = "List of places with queue stats")
    public ResponseEntity<List<PlaceWithQueueDTO>> getPlacesWithQueueStats() {
        String adminId = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(adminService.getPlacesWithQueueStats(adminId));
    }

    @GetMapping("/report/pdf")
    @AdminOnly
    @Operation(summary = "Export admin report as PDF")
    public ResponseEntity<ByteArrayResource> exportAdminReportPdf() {
        String adminId = SecurityContextHolder.getContext().getAuthentication().getName();
        AdminReportDTO report = adminService.getAdminReport(adminId);
        try {
            byte[] pdfBytes = exportService.exportAdminReportToPdf(report);
            String filename = "admin-report-" + LocalDate.now() + ".pdf";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(pdfBytes.length)
                    .body(new ByteArrayResource(pdfBytes));
        } catch (DocumentException e) {
            log.error("Failed to generate admin report PDF", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/report/excel")
    @AdminOnly
    public ResponseEntity<ByteArrayResource> exportAdminReportExcel() {
        String adminId = SecurityContextHolder.getContext().getAuthentication().getName();
        AdminReportDTO report = adminService.getAdminReport(adminId);
        try {
            byte[] excelBytes = exportService.exportAdminReportToExcel(report);
            String filename = "admin-report-" + LocalDate.now() + ".xlsx";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .contentLength(excelBytes.length)
                    .body(new ByteArrayResource(excelBytes));
        } catch (IOException e) {
            log.error("Failed to generate admin report Excel", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/alert-config")
    @AdminOnly
    @Operation(summary = "Create or update alert configuration")
    public ResponseEntity<AlertConfig> saveAlertConfig(
            @RequestParam int thresholdWaitTime,
            @RequestParam(required = false) String notificationEmail) {
        String adminId = SecurityContextHolder.getContext().getAuthentication().getName();
        AlertConfig config = alertConfigService.createOrUpdateConfig(adminId, thresholdWaitTime, notificationEmail);
        return ResponseEntity.ok(config);
    }

    @GetMapping("/alert-config")
    @AdminOnly
    @Operation(summary = "Get current alert configuration")
    public ResponseEntity<AlertConfig> getAlertConfig() {
        String adminId = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            AlertConfig config = alertConfigService.getConfig(adminId);
            return ResponseEntity.ok(config);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/alert-config")
    @AdminOnly
    @Operation(summary = "Delete alert configuration")
    public ResponseEntity<Void> deleteAlertConfig() {
        String adminId = SecurityContextHolder.getContext().getAuthentication().getName();
        alertConfigService.deleteConfig(adminId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/alert-config/toggle")
    @AdminOnly
    @Operation(summary = "Enable or disable alerts")
    public ResponseEntity<Void> toggleAlerts(@RequestParam boolean enabled) {
        String adminId = SecurityContextHolder.getContext().getAuthentication().getName();
        alertConfigService.toggleEnabled(adminId, enabled);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/providers/{providerId}")
    @AdminOnly
    @Operation(summary = "Get provider details", description = "Returns detailed information about a specific provider, including statistics and managed places.")
    @ApiResponse(responseCode = "200", description = "Provider details",
            content = @Content(schema = @Schema(implementation = ProviderDetailsDTO.class)))
    @ApiResponse(responseCode = "403", description = "Forbidden – not the admin of this provider")
    @ApiResponse(responseCode = "404", description = "Provider not found")
    public ResponseEntity<ProviderDetailsDTO> getProviderDetails(@PathVariable String providerId) {
        String adminId = SecurityContextHolder.getContext().getAuthentication().getName();
        ProviderDetailsDTO details = adminService.getProviderById(providerId, adminId);
        return ResponseEntity.ok(details);
    }

    @PutMapping("/providers/{providerId}")
    @AdminOnly
    @Operation(summary = "Update provider details", description = "Updates an existing provider's information. Only the owning admin can update.")
    @ApiResponse(responseCode = "200", description = "Provider updated",
            content = @Content(schema = @Schema(implementation = ProviderDetailsDTO.class)))
    @ApiResponse(responseCode = "400", description = "Invalid input (e.g., place not owned)")
    @ApiResponse(responseCode = "403", description = "Forbidden – not the admin of this provider")
    @ApiResponse(responseCode = "404", description = "Provider not found")
    public ResponseEntity<ProviderDetailsDTO> updateProvider(
            @PathVariable String providerId,
            @Valid @RequestBody ProviderUpdateRequest request) {
        String adminId = SecurityContextHolder.getContext().getAuthentication().getName();
        ProviderDetailsDTO updated = adminService.updateProvider(providerId, request, adminId);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/providers/{providerId}/status")
    @AdminOnly
    @Operation(summary = "Toggle provider active status", description = "Enable or disable a provider account.")
    @ApiResponse(responseCode = "200", description = "Status updated",
            content = @Content(schema = @Schema(implementation = ProviderDetailsDTO.class)))
    @ApiResponse(responseCode = "403", description = "Forbidden – not the admin of this provider")
    @ApiResponse(responseCode = "404", description = "Provider not found")
    public ResponseEntity<ProviderDetailsDTO> toggleProviderStatus(
            @PathVariable String providerId,
            @RequestParam boolean active) {
        String adminId = SecurityContextHolder.getContext().getAuthentication().getName();
        ProviderDetailsDTO updated = adminService.toggleProviderStatus(providerId, active, adminId);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/providers/{providerId}/reset-password")
    @AdminOnly
    @Operation(summary = "Reset provider password", description = "Sends a password reset email to the provider.")
    @ApiResponse(responseCode = "200", description = "Reset email sent")
    @ApiResponse(responseCode = "403", description = "Forbidden – not the admin of this provider")
    @ApiResponse(responseCode = "404", description = "Provider not found")
    @ApiResponse(responseCode = "500", description = "Email sending failed")
    public ResponseEntity<?> resetProviderPassword(@PathVariable String providerId) throws MessagingException {
        String adminId = SecurityContextHolder.getContext().getAuthentication().getName();
        adminService.resetProviderPassword(providerId, adminId);
        return ResponseEntity.ok("Password reset email sent to provider");
    }
}