package com.queueless.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceTest {

    private FileStorageService fileStorageService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageService(tempDir.toString());
    }

    @Test
    void storeFile_Success() {
        MultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );
        String url = fileStorageService.storeFile(file, "user123");
        assertNotNull(url);
        assertTrue(url.startsWith("/uploads/"));
        assertTrue(url.contains("user123_"));
        assertTrue(url.endsWith(".jpg"));
    }

    @Test
    void storeFile_InvalidType() {
        MultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "text content".getBytes()
        );
        assertThrows(IllegalArgumentException.class, () -> fileStorageService.storeFile(file, "user123"));
    }
}