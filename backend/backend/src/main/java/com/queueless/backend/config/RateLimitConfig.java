package com.queueless.backend.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
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

    public Bucket resolveBucket(String key) {
        return cache.computeIfAbsent(key, k -> {
            log.debug("Creating new rate limit bucket for key: {}", key);
            Bandwidth limit = Bandwidth.classic(capacity, Refill.greedy(refill, duration));
            return Bucket4j.builder().addLimit(limit).build();
        });
    }
}