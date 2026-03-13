package com.queueless.backend.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Configuration
public class RateLimitConfig {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    @Value("${rate.limit.capacity:100}")
    private int capacity;

    @Value("${rate.limit.refill:100}")
    private int refill;

    @Value("${rate.limit.duration:PT1M}")
    private Duration duration;

    // Specific limits for different endpoint groups
    @Value("${rate.limit.token.capacity:10}")
    private int tokenCapacity;

    @Value("${rate.limit.token.refill:10}")
    private int tokenRefill;

    @Value("${rate.limit.token.duration:PT1M}")
    private Duration tokenDuration;

    @Value("${rate.limit.search.capacity:50}")
    private int searchCapacity;

    @Value("${rate.limit.search.refill:50}")
    private int searchRefill;

    @Value("${rate.limit.search.duration:PT1M}")
    private Duration searchDuration;

    public Bucket resolveBucket(String key, String endpointGroup) {
        return cache.computeIfAbsent(key, k -> {
            log.debug("Creating new rate limit bucket for key: {} (group: {})", key, endpointGroup);
            Bandwidth limit;
            if ("token".equals(endpointGroup)) {
                limit = Bandwidth.classic(tokenCapacity, Refill.greedy(tokenRefill, tokenDuration));
            } else if ("search".equals(endpointGroup)) {
                limit = Bandwidth.classic(searchCapacity, Refill.greedy(searchRefill, searchDuration));
            } else {
                limit = Bandwidth.classic(capacity, Refill.greedy(refill, duration));
            }
            return Bucket.builder().addLimit(limit).build();
        });
    }
}