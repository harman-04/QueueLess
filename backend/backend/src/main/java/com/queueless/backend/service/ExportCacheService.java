package com.queueless.backend.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ExportCacheService {
    private final Map<String, byte[]> exportCache = new ConcurrentHashMap<>();

    public void saveExport(String exportId, byte[] data) {
        exportCache.put(exportId, data);
    }

    public byte[] getExport(String exportId) {
        return exportCache.get(exportId);
    }

    public void removeExport(String exportId) {
        exportCache.remove(exportId);
    }
}