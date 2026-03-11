package com.queueless.backend.service;

import com.queueless.backend.model.Queue;
import com.queueless.backend.repository.QueueRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
public class QueueMetricsService {

    private final QueueRepository queueRepository;
    private final MeterRegistry meterRegistry;

    private final Map<String, AtomicLong> waitingTokensGauges = new HashMap<>();
    private final Map<String, AtomicLong> inServiceTokensGauges = new HashMap<>();
    private final Map<String, AtomicLong> totalServedGauges = new HashMap<>();

    @PostConstruct
    public void init() {
        // Register initial metrics
        updateAllMetrics();
    }

    public void updateAllMetrics() {
        queueRepository.findAll().forEach(queue -> {
            String queueId = queue.getId();
            String placeId = queue.getPlaceId();
            String serviceName = queue.getServiceName();

            Tags tags = Tags.of(
                    "queueId", queueId,
                    "placeId", placeId,
                    "service", serviceName
            );

            long waiting = queue.getTokens().stream()
                    .filter(t -> "WAITING".equals(t.getStatus()))
                    .count();
            long inService = queue.getTokens().stream()
                    .filter(t -> "IN_SERVICE".equals(t.getStatus()))
                    .count();
            long totalServed = queue.getTokens().stream()
                    .filter(t -> "COMPLETED".equals(t.getStatus()))
                    .count();

            waitingTokensGauges.computeIfAbsent(queueId, k -> {
                AtomicLong value = new AtomicLong(waiting);
                Gauge.builder("queue.waiting.tokens", value, AtomicLong::get)
                        .tags(tags)
                        .description("Number of waiting tokens in the queue")
                        .register(meterRegistry);
                return value;
            }).set(waiting);

            inServiceTokensGauges.computeIfAbsent(queueId, k -> {
                AtomicLong value = new AtomicLong(inService);
                Gauge.builder("queue.in.service.tokens", value, AtomicLong::get)
                        .tags(tags)
                        .description("Number of tokens currently being served")
                        .register(meterRegistry);
                return value;
            }).set(inService);

            totalServedGauges.computeIfAbsent(queueId, k -> {
                AtomicLong value = new AtomicLong(totalServed);
                Gauge.builder("queue.total.served", value, AtomicLong::get)
                        .tags(tags)
                        .description("Total tokens served by this queue")
                        .register(meterRegistry);
                return value;
            }).set(totalServed);
        });
    }
}