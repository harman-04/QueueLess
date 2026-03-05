package com.queueless.backend.security;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
public class OtpVerificationStore {

    private static final String CACHE_NAME = "verifiedEmails";

    @Cacheable(value = CACHE_NAME, key = "#email", unless = "#result == false")
    public boolean isVerified(String email) {
        // If not present in cache, return false
        return false;
    }

    @CachePut(value = CACHE_NAME, key = "#email")
    public boolean markVerified(String email) {
        return true;
    }

    @CacheEvict(value = CACHE_NAME, key = "#email")
    public void remove(String email) {
        // Evict from cache
    }
}