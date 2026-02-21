package com.queueless.backend.service;

import com.queueless.backend.model.Queue;
import com.queueless.backend.model.QueueToken;
import com.queueless.backend.model.UserQueueDetails;
import com.itextpdf.text.DocumentException;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ExportServiceTest {

    @InjectMocks
    private ExportService exportService;

    private Queue testQueue;
    private final String queueId = "queue123";
    private final String userId = "user123";
    private final String providerId = "provider123";
    private final String placeId = "place123";
    private final String serviceId = "service123";

    @BeforeEach
    void setUp() {
        testQueue = new Queue(providerId, "Test Service", placeId, serviceId);
        testQueue.setId(queueId);
        testQueue.setIsActive(true);
        testQueue.setEstimatedWaitTime(15);
        testQueue.setTokens(new ArrayList<>());

        // Add a few tokens
        QueueToken token1 = new QueueToken("T-001", userId, "John Doe", "WAITING", LocalDateTime.now().minusMinutes(10));
        token1.setServedAt(LocalDateTime.now().minusMinutes(5));
        token1.setCompletedAt(LocalDateTime.now());

        QueueToken token2 = new QueueToken("T-002", "user456", "Jane Smith", "COMPLETED", LocalDateTime.now().minusHours(1));
        token2.setServedAt(LocalDateTime.now().minusMinutes(50));
        token2.setCompletedAt(LocalDateTime.now().minusMinutes(10));

        // Add user details to token1
        UserQueueDetails details = new UserQueueDetails();
        details.setPurpose("Consultation");
        details.setCondition("Fever");
        details.setNotes("Bring ID");
        token1.setUserDetails(details);

        testQueue.getTokens().add(token1);
        testQueue.getTokens().add(token2);
    }

    // ================= PDF EXPORT TESTS =================

    @Test
    void exportQueueToPdfTokensReportSuccess() throws DocumentException {
        byte[] pdfBytes = exportService.exportQueueToPdf(testQueue, "tokens", false);
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }

    @Test
    void exportQueueToPdfStatisticsReportSuccess() throws DocumentException {
        byte[] pdfBytes = exportService.exportQueueToPdf(testQueue, "statistics", false);
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }

    @Test
    void exportQueueToPdfFullReportSuccess() throws DocumentException {
        byte[] pdfBytes = exportService.exportQueueToPdf(testQueue, "full", true);
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }

    @Test
    void exportQueueToPdfWithIncludeUserDetails() throws DocumentException {
        byte[] pdfBytes = exportService.exportQueueToPdf(testQueue, "tokens", true);
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }

    @Test
    void exportQueueToPdfInvalidReportTypeThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
                exportService.exportQueueToPdf(testQueue, "invalid", false));
    }

    @Test
    void exportQueueToPdfNullQueueThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
                exportService.exportQueueToPdf(null, "tokens", false));
    }

    // ================= EXCEL EXPORT TESTS =================

    @Test
    void exportQueueToExcelTokensReportSuccess() throws IOException {
        byte[] excelBytes = exportService.exportQueueToExcel(testQueue, "tokens", false);
        assertNotNull(excelBytes);
        assertTrue(excelBytes.length > 0);
    }

    @Test
    void exportQueueToExcelStatisticsReportSuccess() throws IOException {
        byte[] excelBytes = exportService.exportQueueToExcel(testQueue, "statistics", false);
        assertNotNull(excelBytes);
        assertTrue(excelBytes.length > 0);
    }

    @Test
    void exportQueueToExcelFullReportSuccess() throws IOException {
        byte[] excelBytes = exportService.exportQueueToExcel(testQueue, "full", true);
        assertNotNull(excelBytes);
        assertTrue(excelBytes.length > 0);
    }

    @Test
    void exportQueueToExcelWithIncludeUserDetails() throws IOException {
        byte[] excelBytes = exportService.exportQueueToExcel(testQueue, "tokens", true);
        assertNotNull(excelBytes);
        assertTrue(excelBytes.length > 0);
    }

    @Test
    void exportQueueToExcelInvalidReportTypeThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
                exportService.exportQueueToExcel(testQueue, "invalid", false));
    }

    @Test
    void exportQueueToExcelNullQueueThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
                exportService.exportQueueToExcel(null, "tokens", false));
    }
}