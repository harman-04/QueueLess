package com.queueless.backend.controller;

import com.queueless.backend.service.ExportCacheService;
import com.queueless.backend.service.ExportService;
import com.queueless.backend.service.QueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.queueless.backend.exception.ResourceNotFoundException;
import com.itextpdf.text.DocumentException;
import com.queueless.backend.security.annotations.AdminOrProviderOnly; // New import

import java.io.IOException;
import java.util.Date;

/**
 * REST controller for handling export requests for queue data.
 * It provides endpoints to export queue information to PDF and Excel formats.
 * Robust logging is added to track request lifecycles, and exception handling is improved.
 */
@Slf4j
@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
public class ExportController {

    private final ExportService exportService;
    private final QueueService queueService;
    private final ExportCacheService exportCacheService;

    /**
     * Exports queue data to a PDF file.
     *
     * @param queueId The ID of the queue to export.
     * @param reportType The type of report to generate (e.g., "tokens", "statistics", "full").
     * @return A ResponseEntity containing the PDF file as a byte array.
     */
    @GetMapping("/queue/{queueId}/pdf")
    @AdminOrProviderOnly
    public ResponseEntity<ByteArrayResource> exportQueueToPdf(
            @PathVariable String queueId,
            @RequestParam(defaultValue = "tokens") String reportType,
            @RequestParam(defaultValue = "false") Boolean includeUserDetails) {

        log.info("Received PDF export request for queueId: {} with reportType: {}", queueId, reportType);

        try {
            var queue = queueService.getQueueById(queueId);
            log.debug("Found queue with ID: {} and ServiceName: {}", queue.getId(), queue.getServiceName());

            byte[] pdfBytes = exportService.exportQueueToPdf(queue, reportType, includeUserDetails);
            log.info("PDF file successfully generated for queueId: {}. File size: {} bytes", queueId, pdfBytes.length);

            String filename = String.format("queue-report-%s-%s.pdf",
                    queue.getServiceName().replaceAll("\\s+", "-"),
                    new Date().getTime());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(pdfBytes.length)
                    .body(new ByteArrayResource(pdfBytes));

        } catch (ResourceNotFoundException e) {
            log.warn("PDF export failed: Queue with ID {} not found.", queueId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (DocumentException | IllegalArgumentException e) {
            log.error("Failed to generate PDF for queueId: {}. Error: {}", queueId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("An unexpected error occurred during PDF export for queueId: {}. Error: {}", queueId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Exports queue data to an Excel file.
     *
     * @param queueId The ID of the queue to export.
     * @param reportType The type of report to generate (e.g., "tokens", "statistics", "full").
     * @return A ResponseEntity containing the Excel file as a byte array.
     */
    @GetMapping("/queue/{queueId}/excel")
    @AdminOrProviderOnly
    public ResponseEntity<ByteArrayResource> exportQueueToExcel(
            @PathVariable String queueId,
            @RequestParam(defaultValue = "tokens") String reportType,
            @RequestParam(defaultValue = "false") Boolean includeUserDetails) {

        log.info("Received Excel export request for queueId: {} with reportType: {}", queueId, reportType);

        try {
            var queue = queueService.getQueueById(queueId);
            log.debug("Found queue with ID: {} and ServiceName: {}", queue.getId(), queue.getServiceName());

            byte[] excelBytes = exportService.exportQueueToExcel(queue, reportType, includeUserDetails);
            log.info("Excel file successfully generated for queueId: {}. File size: {} bytes", queueId, excelBytes.length);

            String filename = String.format("queue-report-%s-%s.xlsx",
                    queue.getServiceName().replaceAll("\\s+", "-"),
                    new Date().getTime());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .contentLength(excelBytes.length)
                    .body(new ByteArrayResource(excelBytes));

        } catch (ResourceNotFoundException e) {
            log.warn("Excel export failed: Queue with ID {} not found.", queueId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IllegalArgumentException | IOException e) {
            log.error("Failed to generate Excel for queueId: {}. Error: {}", queueId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("An unexpected error occurred during Excel export for queueId: {}. Error: {}", queueId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @GetMapping("/exports/{exportId}")
    @AdminOrProviderOnly
    public ResponseEntity<ByteArrayResource> downloadExport(@PathVariable String exportId) {
        try {
            byte[] data = exportCacheService.getExport(exportId);
            if (data == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + exportId + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(data.length)
                    .body(new ByteArrayResource(data));
        } catch (Exception e) {
            log.error("Error downloading export: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}