package com.queueless.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ExportCacheServiceTest {

    private ExportCacheService exportCacheService;

    private final String exportId = "test-export-123";
    private final byte[] testData = "Hello, PDF Content".getBytes();
    private final String filename = "report.pdf";
    private final String queueId = "queue-001";
    private final String reportType = "full";
    private final String format = "pdf";

    @BeforeEach
    void setUp() {
        // Since ExportCacheService has no dependencies, we initialize it directly
        exportCacheService = new ExportCacheService();
    }

    @Test
    void saveAndGetExportSuccess() {
        // Act
        exportCacheService.saveExport(exportId, testData, filename, queueId, reportType, format);
        ExportCacheService.ExportEntry entry = exportCacheService.getExport(exportId);

        // Assert
        assertNotNull(entry);
        assertArrayEquals(testData, entry.getData());
        assertEquals(filename, entry.getFilename());
        assertEquals(queueId, entry.getQueueId());
        assertEquals(reportType, entry.getReportType());
        assertEquals(format, entry.getFormat());
        assertNotNull(entry.getCreatedAt());
    }

    @Test
    void getExportNotFoundReturnsNull() {
        // Act
        ExportCacheService.ExportEntry entry = exportCacheService.getExport("non-existent-id");

        // Assert
        assertNull(entry);
    }

    @Test
    void removeExportSuccess() {
        // Arrange
        exportCacheService.saveExport(exportId, testData, filename, queueId, reportType, format);

        // Act
        exportCacheService.removeExport(exportId);
        ExportCacheService.ExportEntry entry = exportCacheService.getExport(exportId);

        // Assert
        assertNull(entry);
    }

    @Test
    void getAllExportsReturnsAllEntries() {
        // Arrange
        exportCacheService.saveExport("id1", testData, "file1.pdf", "q1", "full", "pdf");
        exportCacheService.saveExport("id2", testData, "file2.xlsx", "q2", "summary", "excel");

        // Act
        Map<String, ExportCacheService.ExportEntry> allExports = exportCacheService.getAllExports();

        // Assert
        assertEquals(2, allExports.size());
        assertTrue(allExports.containsKey("id1"));
        assertTrue(allExports.containsKey("id2"));
    }

    @Test
    void saveExportOverwriteExistingId() {
        // Arrange
        exportCacheService.saveExport(exportId, testData, "old.pdf", queueId, reportType, format);
        byte[] newData = "New Content".getBytes();

        // Act
        exportCacheService.saveExport(exportId, newData, "new.pdf", queueId, reportType, format);
        ExportCacheService.ExportEntry entry = exportCacheService.getExport(exportId);

        // Assert
        assertEquals("new.pdf", entry.getFilename());
        assertArrayEquals(newData, entry.getData());
    }
}