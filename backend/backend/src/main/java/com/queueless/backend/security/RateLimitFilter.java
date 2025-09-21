// C:\Users\manji\Downloads\QueueLess\backend\src\main\java\com\queueless\backend\security\RateLimitFilter.java

package com.queueless.backend.security;

import com.queueless.backend.config.RateLimitConfig;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j // This is a Lombok annotation that creates a logger
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitConfig rateLimitConfig;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String requestURI = request.getRequestURI();

        // Apply rate limiting only to specific paths
        if (requestURI.startsWith("/api/auth/") ||
                requestURI.startsWith("/api/password/") ||
                requestURI.matches("/api/queues/[^/]+/add-token") ||
                requestURI.startsWith("/api/places")) {

            String ipAddress = request.getRemoteAddr();
            String key = ipAddress + ":" + request.getRequestURI();

            log.info("Rate limiting request for key: {}", key); // Log the key being used

            try {
                Bucket bucket = rateLimitConfig.resolveBucket(key);
                ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

                if (probe.isConsumed()) {
                    log.info("Request consumed. Remaining tokens: {}", probe.getRemainingTokens()); // Log successful consumption
                    response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
                    filterChain.doFilter(request, response);
                } else {
                    log.warn("Rate limit exceeded for key: {}", key); // Log the rate limit event
                    long waitForRefill = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill());
                    response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitForRefill));
                    response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(), "You have exhausted your API Request Quota");
                }
            } catch (Exception e) {
                // This catch block is crucial. It will tell you if the bucket logic itself is failing.
                log.error("An error occurred during rate limiting for key: {}. Error: {}", key, e.getMessage(), e);
                // Pass the request along to allow it to be processed normally if the rate limiter fails.
                filterChain.doFilter(request, response);
            }
        } else {
            filterChain.doFilter(request, response);
        }
    }
}