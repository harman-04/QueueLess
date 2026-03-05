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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitConfig rateLimitConfig;

    @Value("${rate.limit.paths:/api/auth/**,/api/password/**,/api/queues/*/add-token,/api/places/**}")
    private List<String> limitedPaths;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestURI = request.getRequestURI();

        if (isPathLimited(requestURI)) {
            String ipAddress = request.getRemoteAddr();
            String key = ipAddress + ":" + request.getRequestURI();

            log.debug("Rate limiting request for key: {}", key);

            try {
                Bucket bucket = rateLimitConfig.resolveBucket(key);
                ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

                if (probe.isConsumed()) {
                    log.debug("Request consumed. Remaining tokens: {}", probe.getRemainingTokens());
                    response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
                    filterChain.doFilter(request, response);
                } else {
                    log.warn("Rate limit exceeded for key: {}", key);
                    long waitForRefill = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill());
                    response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitForRefill));
                    response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(),
                            "You have exhausted your API Request Quota");
                }
            } catch (Exception e) {
                log.error("An error occurred during rate limiting for key: {}. Error: {}", key, e.getMessage(), e);
                // Fall through – allow request if rate limiter fails
                filterChain.doFilter(request, response);
            }
        } else {
            filterChain.doFilter(request, response);
        }
    }

    private boolean isPathLimited(String uri) {
        for (String pattern : limitedPaths) {
            if (matches(pattern, uri)) {
                return true;
            }
        }
        return false;
    }

    private boolean matches(String pattern, String uri) {
        // Convert ant-style pattern to regex
        String regex = pattern
                .replace(".", "\\.")
                .replace("**", ".*")
                .replace("*", "[^/]*");
        return Pattern.matches(regex, uri);
    }
}