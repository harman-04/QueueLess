package com.queueless.backend.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ExportCacheService {
    private final Map<String, ExportEntry> exportCache = new ConcurrentHashMap<>();

    @Data
    public static class ExportEntry {
        private byte[] data;
        private String filename;
        private LocalDateTime createdAt;
        private String queueId;
        private String reportType;
        private String format;

        public ExportEntry(byte[] data, String filename, String queueId, String reportType, String format) {
            this.data = data;
            this.filename = filename;
            this.createdAt = LocalDateTime.now();
            this.queueId = queueId;
            this.reportType = reportType;
            this.format = format;
        }
    }

    public void saveExport(String exportId, byte[] data, String filename, String queueId, String reportType, String format) {
        exportCache.put(exportId, new ExportEntry(data, filename, queueId, reportType, format));
        log.debug("Saved export: {} (queue: {}, format: {})", exportId, queueId, format);
    }

    public ExportEntry getExport(String exportId) {
        return exportCache.get(exportId);
    }

    public void removeExport(String exportId) {
        exportCache.remove(exportId);
        log.debug("Removed export: {}", exportId);
    }

    public Map<String, ExportEntry> getAllExports() {
        return exportCache;
    }
}