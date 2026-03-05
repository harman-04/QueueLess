package com.queueless.backend.controller;

import com.queueless.backend.service.ExportCacheService;
import com.queueless.backend.service.ExportService;
import com.queueless.backend.service.QueueService;
import com.queueless.backend.exception.ResourceNotFoundException;
import com.queueless.backend.security.annotations.AdminOrProviderOnly;
import com.itextpdf.text.DocumentException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
@Tag(name = "Export", description = "Endpoints for exporting queue data (PDF/Excel)")
public class ExportController {

    private final ExportService exportService;
    private final QueueService queueService;
    private final ExportCacheService exportCacheService;

    @GetMapping("/queue/{queueId}/pdf")
    @AdminOrProviderOnly
    @Operation(summary = "Export queue to PDF", description = "Generates a PDF report for a queue. Report type can be 'tokens', 'statistics', or 'full'.")
    @ApiResponse(responseCode = "200", description = "PDF generated",
            content = @Content(mediaType = "application/pdf"))
    @ApiResponse(responseCode = "400", description = "Invalid report type")
    @ApiResponse(responseCode = "404", description = "Queue not found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public ResponseEntity<ByteArrayResource> exportQueueToPdf(
            @Parameter(description = "Queue ID") @PathVariable String queueId,
            @Parameter(description = "Report type: tokens, statistics, full") @RequestParam(defaultValue = "tokens") String reportType,
            @Parameter(description = "Include user details in the report") @RequestParam(defaultValue = "false") Boolean includeUserDetails) {

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

    @GetMapping("/queue/{queueId}/excel")
    @AdminOrProviderOnly
    @Operation(summary = "Export queue to Excel", description = "Generates an Excel report for a queue. Report type can be 'tokens', 'statistics', or 'full'.")
    @ApiResponse(responseCode = "200", description = "Excel generated",
            content = @Content(mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
    @ApiResponse(responseCode = "400", description = "Invalid report type")
    @ApiResponse(responseCode = "404", description = "Queue not found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public ResponseEntity<ByteArrayResource> exportQueueToExcel(
            @Parameter(description = "Queue ID") @PathVariable String queueId,
            @Parameter(description = "Report type: tokens, statistics, full") @RequestParam(defaultValue = "tokens") String reportType,
            @Parameter(description = "Include user details in the report") @RequestParam(defaultValue = "false") Boolean includeUserDetails) {

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
    @Operation(summary = "Download a previously exported file", description = "Retrieves a cached export file by its ID.")
    @ApiResponse(responseCode = "200", description = "File downloaded")
    @ApiResponse(responseCode = "404", description = "Export not found")
    public ResponseEntity<ByteArrayResource> downloadExport(@PathVariable String exportId) {
        ExportCacheService.ExportEntry entry = exportCacheService.getExport(exportId);
        if (entry == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + entry.getFilename())
                .contentType(entry.getFormat().equals("pdf") ? MediaType.APPLICATION_PDF : MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(entry.getData().length)
                .body(new ByteArrayResource(entry.getData()));
    }

    @GetMapping("/exports")
    @AdminOrProviderOnly
    @Operation(summary = "List all cached exports", description = "Returns metadata of all exports stored in the cache.")
    @ApiResponse(responseCode = "200", description = "List of exports")
    public ResponseEntity<List<Map<String, Object>>> listExports(Authentication authentication) {
        String userId = authentication.getName();
        Map<String, ExportCacheService.ExportEntry> all = exportCacheService.getAllExports();
        List<Map<String, Object>> list = all.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("exportId", entry.getKey());
                    map.put("filename", entry.getValue().getFilename());
                    map.put("createdAt", entry.getValue().getCreatedAt());
                    map.put("queueId", entry.getValue().getQueueId());
                    map.put("reportType", entry.getValue().getReportType());
                    map.put("format", entry.getValue().getFormat());
                    return map;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }
}