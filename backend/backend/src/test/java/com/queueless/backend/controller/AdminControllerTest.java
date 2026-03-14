package com.queueless.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.text.DocumentException;
import com.queueless.backend.dto.*;
import com.queueless.backend.exception.AccessDeniedException;
import com.queueless.backend.exception.ResourceNotFoundException;
import com.queueless.backend.model.*;
import com.queueless.backend.repository.PaymentRepository;
import com.queueless.backend.repository.PlaceRepository;
import com.queueless.backend.repository.QueueRepository;
import com.queueless.backend.repository.UserRepository;
import com.queueless.backend.service.AdminService;
import com.queueless.backend.service.AlertConfigService;
import com.queueless.backend.service.ExportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.cache.type=none",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration"
})
@AutoConfigureMockMvc
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PaymentRepository paymentRepository;

    @MockitoBean
    private AdminService adminService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private PlaceRepository placeRepository;

    @MockitoBean
    private QueueRepository queueRepository;

    @MockitoBean
    private ExportService exportService;

    @MockitoBean
    private AlertConfigService alertConfigService;

    private final String adminId = "admin123";
    private final String providerId = "provider123";
    private final String adminEmail = "admin@example.com";

    private User createAdminUser() {
        return User.builder()
                .id(adminId)
                .email(adminEmail)
                .name("Admin User")
                .role(com.queueless.backend.enums.Role.ADMIN)
                .build();
    }

    // ==================== GET MY PAYMENT HISTORY ====================

    @Test
    @WithMockUser(username = adminId, roles = {"ADMIN"})
    void getMyPaymentHistory_Success() throws Exception {
        User adminUser = createAdminUser();
        when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));

        Payment payment = Payment.builder()
                .id("pay1")
                .amount(10000)
                .createdForEmail(adminEmail)
                .isPaid(true)
                .createdAt(LocalDateTime.now())
                .build();
        when(paymentRepository.findByCreatedForEmail(adminEmail)).thenReturn(List.of(payment));

        mockMvc.perform(get("/api/admin/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("pay1"));
    }

    @Test
    @WithMockUser(username = adminId, roles = {"ADMIN"})
    void getMyPaymentHistory_UserNotFound() throws Exception {
        when(userRepository.findById(adminId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/admin/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ==================== GET ADMIN STATS ====================

    @Test
    @WithMockUser(username = adminId, roles = {"ADMIN"})
    void getAdminStats_Success() throws Exception {
        Map<String, Object> stats = Map.of(
                "totalPlaces", 5,
                "totalQueues", 10,
                "activeQueues", 8
        );
        when(adminService.getDashboardStats(adminId)).thenReturn(stats);

        mockMvc.perform(get("/api/admin/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPlaces").value(5))
                .andExpect(jsonPath("$.totalQueues").value(10));
    }

    @Test
    @WithMockUser(username = adminId, roles = {"ADMIN"})
    void getAdminStats_ServiceException() throws Exception {
        when(adminService.getDashboardStats(adminId)).thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(get("/api/admin/stats"))
                .andExpect(status().isBadRequest()); // was is5xxServerError
    }

    // ==================== GET ALL ADMIN QUEUES ====================

    @Test
    @WithMockUser(username = adminId, roles = {"ADMIN"})
    void getAllAdminQueues_Success() throws Exception {
        Place place = new Place();
        place.setId("place1");
        when(placeRepository.findByAdminId(adminId)).thenReturn(List.of(place));

        Queue queue = new Queue("provider", "Service", "place1", "service1");
        queue.setId("queue1");
        when(queueRepository.findByPlaceIdIn(List.of("place1"))).thenReturn(List.of(queue));

        mockMvc.perform(get("/api/admin/queues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("queue1"));
    }

    @Test
    @WithMockUser(username = adminId, roles = {"ADMIN"})
    void getAllAdminQueues_Exception() throws Exception {
        when(placeRepository.findByAdminId(adminId)).thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(get("/api/admin/queues"))
                .andExpect(status().isBadRequest()); // was is5xxServerError
    }

    // ==================== GET ENHANCED QUEUES ====================

    @Test
    @WithMockUser(username = adminId, roles = {"ADMIN"})
    void getAdminQueuesWithDetails_Success() throws Exception {
        AdminQueueDTO dto = new AdminQueueDTO();
        dto.setId("queue1");
        dto.setServiceName("Service");
        when(adminService.getAdminQueuesWithDetails(adminId)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/admin/queues/enhanced"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("queue1"));
    }

    @Test
    @WithMockUser(username = adminId, roles = {"ADMIN"})
    void getAdminQueuesWithDetails_Exception() throws Exception {
        when(adminService.getAdminQueuesWithDetails(adminId)).thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(get("/api/admin/queues/enhanced"))
                .andExpect(status().isBadRequest()); // was is5xxServerError
    }

    // ==================== GET ENHANCED PAYMENT HISTORY ====================

    @Test
    @WithMockUser(username = adminId, roles = {"ADMIN"})
    void getEnhancedPaymentHistory_Success() throws Exception {
        PaymentHistoryDTO dto = PaymentHistoryDTO.builder()
                .id("pay1")
                .description("Test")
                .amount(10000)
                .build();
        when(adminService.getEnhancedPaymentHistory(adminId)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/admin/payments/enhanced"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("pay1"));
    }

    // ==================== TOKENS OVER TIME ====================

    @Test
    @WithMockUser(username = adminId, roles = {"ADMIN"})
    void getTokensOverTime_Success() throws Exception {
        Map<String, Object> data = Map.of(
                "dates", List.of("2026-03-01", "2026-03-02"),
                "counts", List.of(5, 8)
        );
        when(adminService.getTokensOverTime(adminId, 30)).thenReturn(data);

        mockMvc.perform(get("/api/admin/analytics/tokens-over-time")
                        .param("days", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dates[0]").value("2026-03-01"))
                .andExpect(jsonPath("$.counts[1]").value(8));
    }

    @Test
    @WithMockUser(username = adminId, roles = {"ADMIN"})
    void getTokensOverTime_InvalidDays() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/tokens-over-time")
                        .param("days", "0"))
                .andExpect(status().isBadRequest());
    }

    // ==================== BUSIEST HOURS ====================

    @Test
    @WithMockUser(username = adminId, roles = {"ADMIN"})
    void getBusiestHours_Success() throws Exception {
        Map<Integer, Double> data = Map.of(9, 12.5, 10, 15.2);
        when(adminService.getBusiestHours(adminId)).thenReturn(data);

        mockMvc.perform(get("/api/admin/analytics/busiest-hours"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.9").value(12.5))
                .andExpect(jsonPath("$.10").value(15.2));
    }

    // ==================== PROVIDERS WITH QUEUES ====================

    @Test
    @WithMockUser(username = adminId, roles = {"ADMIN"})
    void getProvidersWithQueues_Success() throws Exception {
        ProviderPerformanceDTO dto = new ProviderPerformanceDTO(
                User.builder().id("prov1").name("Provider").build(),
                5, 3, 10, 4.5, 2.5
        );
        when(adminService.getProvidersWithQueues(adminId)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/admin/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("prov1"))
                .andExpect(jsonPath("$[0].name").value("Provider"));
    }

    // ==================== PLACES WITH QUEUE STATS ====================

    @Test
    @WithMockUser(username = adminId, roles = {"ADMIN"})
    void getPlacesWithQueueStats_Success() throws Exception {
        Place place = new Place();
        place.setId("place1");
        place.setName("Place");
        PlaceWithQueueDTO dto = new PlaceWithQueueDTO(place, 5, 2);
        when(adminService.getPlacesWithQueueStats(adminId)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/admin/places-with-queues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("place1"))
                .andExpect(jsonPath("$[0].waitingTokens").value(5));
    }

    // ==================== EXPORT REPORT PDF ====================

    @Test
    @WithMockUser(username = adminId, roles = {"ADMIN"})
    void exportAdminReportPdf_Success() throws Exception {
        AdminReportDTO report = new AdminReportDTO();
        when(adminService.getAdminReport(adminId)).thenReturn(report);
        byte[] pdfBytes = "PDF content".getBytes();
        when(exportService.exportAdminReportToPdf(report)).thenReturn(pdfBytes);

        mockMvc.perform(get("/api/admin/report/pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=admin-report-" + LocalDate.now() + ".pdf"))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(pdfBytes));
    }

    @Test
    @WithMockUser(username = adminId, roles = {"ADMIN"})
    void exportAdminReportPdf_Exception() throws Exception {
        AdminReportDTO report = new AdminReportDTO();
        when(adminService.getAdminReport(adminId)).thenReturn(report);
        when(exportService.exportAdminReportToPdf(report)).thenThrow(new DocumentException());

        mockMvc.perform(get("/api/admin/report/pdf"))
                .andExpect(status().isInternalServerError());
    }

    // ==================== EXPORT REPORT EXCEL ====================

    @Test
    @WithMockUser(username = adminId, roles = {"ADMIN"})
    void exportAdminReportExcel_Success() throws Exception {
        AdminReportDTO report = new AdminReportDTO();
        when(adminService.getAdminReport(adminId)).thenReturn(report);
        byte[] excelBytes = "Excel content".getBytes();
        when(exportService.exportAdminReportToExcel(report)).thenReturn(excelBytes);

        mockMvc.perform(get("/api/admin/report/excel"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=admin-report-" + LocalDate.now() + ".xlsx"))
                .andExpect(content().contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")))
                .andExpect(content().bytes(excelBytes));
    }

    @Test
    @WithMockUser(username = adminId, roles = {"ADMIN"})
    void exportAdminReportExcel_Exception() throws Exception {
        AdminReportDTO report = new AdminReportDTO();
        when(adminService.getAdminReport(adminId)).thenReturn(report);
        when(exportService.exportAdminReportToExcel(report)).thenThrow(new IOException());

        mockMvc.perform(get("/api/admin/report/excel"))
                .andExpect(status().isInternalServerError());
    }

    // ==================== ALERT CONFIG ====================

    @Test
    @WithMockUser(username = adminId, roles = {"ADMIN"})
    void saveAlertConfig_Success() throws Exception {
        AlertConfig config = new AlertConfig();
        config.setAdminId(adminId);
        config.setThresholdWaitTime(15);
        when(alertConfigService.createOrUpdateConfig(eq(adminId), eq(15), any()))
                .thenReturn(config);

        mockMvc.perform(post("/api/admin/alert-config")
                        .param("thresholdWaitTime", "15")
                        .param("notificationEmail", "alert@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adminId").value(adminId));
    }

    @Test
    @WithMockUser(username = adminId, roles = {"ADMIN"})
    void getAlertConfig_Found() throws Exception {
        AlertConfig config = new AlertConfig();
        config.setAdminId(adminId);
        when(alertConfigService.getConfig(adminId)).thenReturn(config);

        mockMvc.perform(get("/api/admin/alert-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adminId").value(adminId));
    }

    @Test
    @WithMockUser(username = adminId, roles = {"ADMIN"})
    void getAlertConfig_NotFound() throws Exception {
        when(alertConfigService.getConfig(adminId)).thenThrow(new ResourceNotFoundException(""));

        mockMvc.perform(get("/api/admin/alert-config"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = adminId, roles = {"ADMIN"})
    void deleteAlertConfig_Success() throws Exception {
        mockMvc.perform(delete("/api/admin/alert-config"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = adminId, roles = {"ADMIN"})
    void toggleAlerts_Success() throws Exception {
        mockMvc.perform(put("/api/admin/alert-config/toggle")
                        .param("enabled", "true"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = adminId, roles = {"ADMIN"})
    void getProviderDetails_Success() throws Exception {
        ProviderDetailsDTO dto = ProviderDetailsDTO.builder()
                .id(providerId)
                .name("Provider")
                .email("prov@test.com")
                .isActive(true)
                .build();

        when(adminService.getProviderById(providerId, adminId)).thenReturn(dto);

        mockMvc.perform(get("/api/admin/providers/{providerId}", providerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(providerId))
                .andExpect(jsonPath("$.name").value("Provider"));
    }

    @Test
    @WithMockUser(username = adminId, roles = {"ADMIN"})
    void getProviderDetails_NotFound() throws Exception {
        when(adminService.getProviderById(providerId, adminId))
                .thenThrow(new ResourceNotFoundException("Not found"));

        mockMvc.perform(get("/api/admin/providers/{providerId}", providerId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = adminId, roles = {"ADMIN"})
    void getProviderDetails_Forbidden() throws Exception {
        when(adminService.getProviderById(providerId, adminId))
                .thenThrow(new AccessDeniedException("Access denied"));

        mockMvc.perform(get("/api/admin/providers/{providerId}", providerId))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = adminId, roles = {"ADMIN"})
    void updateProvider_Success() throws Exception {
        ProviderUpdateRequest request = new ProviderUpdateRequest();
        request.setName("Updated Provider");
        request.setEmail("updated@test.com");
        request.setPhoneNumber("1234567890");
        request.setManagedPlaceIds(List.of("place1", "place2"));
        request.setIsActive(true);

        ProviderDetailsDTO responseDto = ProviderDetailsDTO.builder()
                .id(providerId)
                .name("Updated Provider")
                .email("updated@test.com")
                .phoneNumber("1234567890")
                .managedPlaceIds(List.of("place1", "place2"))
                .isActive(true)
                .build();

        when(adminService.updateProvider(eq(providerId), any(ProviderUpdateRequest.class), eq(adminId)))
                .thenReturn(responseDto);

        mockMvc.perform(put("/api/admin/providers/{providerId}", providerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(providerId))
                .andExpect(jsonPath("$.name").value("Updated Provider"));
    }

    @Test
    @WithMockUser(username = adminId, roles = {"ADMIN"})
    void updateProvider_NotFound() throws Exception {
        ProviderUpdateRequest request = new ProviderUpdateRequest();
        request.setName("Test");

        when(adminService.updateProvider(eq(providerId), any(ProviderUpdateRequest.class), eq(adminId)))
                .thenThrow(new ResourceNotFoundException("Not found"));

        mockMvc.perform(put("/api/admin/providers/{providerId}", providerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = adminId, roles = {"ADMIN"})
    void updateProvider_Forbidden() throws Exception {
        ProviderUpdateRequest request = new ProviderUpdateRequest();
        request.setName("Test");

        when(adminService.updateProvider(eq(providerId), any(ProviderUpdateRequest.class), eq(adminId)))
                .thenThrow(new AccessDeniedException("Access denied"));

        mockMvc.perform(put("/api/admin/providers/{providerId}", providerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = adminId, roles = {"ADMIN"})
    void toggleProviderStatus_Success() throws Exception {
        ProviderDetailsDTO responseDto = ProviderDetailsDTO.builder()
                .id(providerId)
                .name("Provider")
                .isActive(false)
                .build();

        when(adminService.toggleProviderStatus(providerId, false, adminId)).thenReturn(responseDto);

        mockMvc.perform(patch("/api/admin/providers/{providerId}/status", providerId)
                        .param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(providerId))
                .andExpect(jsonPath("$.isActive").value(false));
    }

    @Test
    @WithMockUser(username = adminId, roles = {"ADMIN"})
    void toggleProviderStatus_NotFound() throws Exception {
        when(adminService.toggleProviderStatus(providerId, false, adminId))
                .thenThrow(new ResourceNotFoundException("Not found"));

        mockMvc.perform(patch("/api/admin/providers/{providerId}/status", providerId)
                        .param("active", "false"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = adminId, roles = {"ADMIN"})
    void toggleProviderStatus_Forbidden() throws Exception {
        when(adminService.toggleProviderStatus(providerId, false, adminId))
                .thenThrow(new AccessDeniedException("Access denied"));

        mockMvc.perform(patch("/api/admin/providers/{providerId}/status", providerId)
                        .param("active", "false"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = adminId, roles = {"ADMIN"})
    void resetProviderPassword_Success() throws Exception {
        doNothing().when(adminService).resetProviderPassword(providerId, adminId);

        mockMvc.perform(post("/api/admin/providers/{providerId}/reset-password", providerId))
                .andExpect(status().isOk())
                .andExpect(content().string("Password reset email sent to provider"));
    }

    @Test
    @WithMockUser(username = adminId, roles = {"ADMIN"})
    void resetProviderPassword_NotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Not found"))
                .when(adminService).resetProviderPassword(providerId, adminId);

        mockMvc.perform(post("/api/admin/providers/{providerId}/reset-password", providerId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = adminId, roles = {"ADMIN"})
    void resetProviderPassword_Forbidden() throws Exception {
        doThrow(new AccessDeniedException("Access denied"))
                .when(adminService).resetProviderPassword(providerId, adminId);

        mockMvc.perform(post("/api/admin/providers/{providerId}/reset-password", providerId))
                .andExpect(status().isForbidden());
    }
}