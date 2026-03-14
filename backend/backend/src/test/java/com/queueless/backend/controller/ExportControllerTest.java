package com.queueless.backend.controller;

import com.itextpdf.text.DocumentException;
import com.queueless.backend.exception.ResourceNotFoundException;
import com.queueless.backend.model.Queue;
import com.queueless.backend.service.ExportCacheService;
import com.queueless.backend.service.ExportService;
import com.queueless.backend.service.QueueService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.cache.type=none",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration"
})
@AutoConfigureMockMvc
class ExportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExportService exportService;

    @MockitoBean
    private QueueService queueService;

    @MockitoBean
    private ExportCacheService exportCacheService;

    private final String queueId = "queue123";
    private final String providerId = "provider123";
    private final String exportId = "export123";
    private final byte[] pdfBytes = "PDF content".getBytes();
    private final byte[] excelBytes = "Excel content".getBytes();

    private Queue createTestQueue() {
        Queue queue = new Queue(providerId, "Test Service", "place123", "service123");
        queue.setId(queueId);
        return queue;
    }

    // ==================== PDF EXPORT ====================

    @Test
    @WithMockUser(username = providerId, roles = {"PROVIDER"})
    void exportQueueToPdf_Success() throws Exception {
        Queue queue = createTestQueue();
        when(queueService.getQueueById(queueId)).thenReturn(queue);
        when(exportService.exportQueueToPdf(any(Queue.class), eq("tokens"), eq(false))).thenReturn(pdfBytes);

        mockMvc.perform(get("/api/export/queue/{queueId}/pdf", queueId)
                        .param("reportType", "tokens")
                        .param("includeUserDetails", "false"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, startsWith("attachment; filename=queue-report-Test-Service-")))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(pdfBytes));
    }

    @Test
    @WithMockUser(username = providerId, roles = {"PROVIDER"})
    void exportQueueToPdf_QueueNotFound() throws Exception {
        when(queueService.getQueueById(queueId)).thenThrow(new ResourceNotFoundException("Queue not found"));

        mockMvc.perform(get("/api/export/queue/{queueId}/pdf", queueId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = providerId, roles = {"PROVIDER"})
    void exportQueueToPdf_DocumentException() throws Exception {
        Queue queue = createTestQueue();
        when(queueService.getQueueById(queueId)).thenReturn(queue);
        when(exportService.exportQueueToPdf(eq(queue), eq("tokens"), eq(false)))
                .thenThrow(new DocumentException("PDF error"));

        mockMvc.perform(get("/api/export/queue/{queueId}/pdf", queueId))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = providerId, roles = {"PROVIDER"})
    void exportQueueToPdf_IllegalArgumentException() throws Exception {
        Queue queue = createTestQueue();
        when(queueService.getQueueById(queueId)).thenReturn(queue);
        when(exportService.exportQueueToPdf(eq(queue), eq("invalid"), eq(false)))
                .thenThrow(new IllegalArgumentException("Invalid report type"));

        mockMvc.perform(get("/api/export/queue/{queueId}/pdf", queueId)
                        .param("reportType", "invalid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = providerId, roles = {"PROVIDER"})
    void exportQueueToPdf_UnexpectedException() throws Exception {
        Queue queue = createTestQueue();
        when(queueService.getQueueById(queueId)).thenReturn(queue);
        when(exportService.exportQueueToPdf(eq(queue), eq("tokens"), eq(false)))
                .thenThrow(new RuntimeException("Unexpected"));

        mockMvc.perform(get("/api/export/queue/{queueId}/pdf", queueId))
                .andExpect(status().isInternalServerError());
    }

    // ==================== EXCEL EXPORT ====================

    @Test
    @WithMockUser(username = providerId, roles = {"PROVIDER"})
    void exportQueueToExcel_Success() throws Exception {
        Queue queue = createTestQueue();
        when(queueService.getQueueById(queueId)).thenReturn(queue);
        when(exportService.exportQueueToExcel(any(Queue.class), eq("tokens"), eq(false))).thenReturn(excelBytes);

        mockMvc.perform(get("/api/export/queue/{queueId}/excel", queueId)
                        .param("reportType", "tokens")
                        .param("includeUserDetails", "false"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, startsWith("attachment; filename=queue-report-Test-Service-")))
                .andExpect(content().contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")))
                .andExpect(content().bytes(excelBytes));
    }
    @Test
    @WithMockUser(username = providerId, roles = {"PROVIDER"})
    void exportQueueToExcel_QueueNotFound() throws Exception {
        when(queueService.getQueueById(queueId)).thenThrow(new ResourceNotFoundException("Queue not found"));

        mockMvc.perform(get("/api/export/queue/{queueId}/excel", queueId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = providerId, roles = {"PROVIDER"})
    void exportQueueToExcel_IOException() throws Exception {
        Queue queue = createTestQueue();
        when(queueService.getQueueById(queueId)).thenReturn(queue);
        when(exportService.exportQueueToExcel(eq(queue), eq("tokens"), eq(false)))
                .thenThrow(new IOException("Excel error"));

        mockMvc.perform(get("/api/export/queue/{queueId}/excel", queueId))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = providerId, roles = {"PROVIDER"})
    void exportQueueToExcel_IllegalArgumentException() throws Exception {
        Queue queue = createTestQueue();
        when(queueService.getQueueById(queueId)).thenReturn(queue);
        when(exportService.exportQueueToExcel(eq(queue), eq("invalid"), eq(false)))
                .thenThrow(new IllegalArgumentException("Invalid report type"));

        mockMvc.perform(get("/api/export/queue/{queueId}/excel", queueId)
                        .param("reportType", "invalid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = providerId, roles = {"PROVIDER"})
    void exportQueueToExcel_UnexpectedException() throws Exception {
        Queue queue = createTestQueue();
        when(queueService.getQueueById(queueId)).thenReturn(queue);
        when(exportService.exportQueueToExcel(eq(queue), eq("tokens"), eq(false)))
                .thenThrow(new RuntimeException("Unexpected"));

        mockMvc.perform(get("/api/export/queue/{queueId}/excel", queueId))
                .andExpect(status().isInternalServerError());
    }

    // ==================== DOWNLOAD EXPORT ====================

    @Test
    @WithMockUser(username = providerId, roles = {"PROVIDER"})
    void downloadExport_Success_Pdf() throws Exception {
        ExportCacheService.ExportEntry entry = new ExportCacheService.ExportEntry(
                pdfBytes, "report.pdf", queueId, "tokens", "pdf"
        );
        when(exportCacheService.getExport(exportId)).thenReturn(entry);

        mockMvc.perform(get("/api/export/exports/{exportId}", exportId))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report.pdf"))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(pdfBytes));
    }

    @Test
    @WithMockUser(username = providerId, roles = {"PROVIDER"})
    void downloadExport_Success_Excel() throws Exception {
        ExportCacheService.ExportEntry entry = new ExportCacheService.ExportEntry(
                excelBytes, "report.xlsx", queueId, "tokens", "excel"
        );
        when(exportCacheService.getExport(exportId)).thenReturn(entry);

        mockMvc.perform(get("/api/export/exports/{exportId}", exportId))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report.xlsx"))
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(content().bytes(excelBytes));
    }

    @Test
    @WithMockUser(username = providerId, roles = {"PROVIDER"})
    void downloadExport_NotFound() throws Exception {
        when(exportCacheService.getExport(exportId)).thenReturn(null);

        mockMvc.perform(get("/api/export/exports/{exportId}", exportId))
                .andExpect(status().isNotFound());
    }

    // ==================== LIST EXPORTS ====================

    @Test
    @WithMockUser(username = providerId, roles = {"PROVIDER"})
    void listExports_Success() throws Exception {
        ExportCacheService.ExportEntry entry1 = new ExportCacheService.ExportEntry(
                pdfBytes, "report1.pdf", queueId, "full", "pdf"
        );
        ExportCacheService.ExportEntry entry2 = new ExportCacheService.ExportEntry(
                excelBytes, "report2.xlsx", queueId, "statistics", "excel"
        );
        when(exportCacheService.getAllExports()).thenReturn(Map.of(
                "exp1", entry1,
                "exp2", entry2
        ));

        mockMvc.perform(get("/api/export/exports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.exportId == 'exp1')].filename").value("report1.pdf"))
                .andExpect(jsonPath("$[?(@.exportId == 'exp2')].filename").value("report2.xlsx"));
    }
}