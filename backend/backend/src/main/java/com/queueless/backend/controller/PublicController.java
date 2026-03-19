package com.queueless.backend.controller;

import com.queueless.backend.model.Queue;
import com.queueless.backend.repository.PlaceRepository;
import com.queueless.backend.repository.QueueRepository;
import com.queueless.backend.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
@Tag(name = "Public", description = "Public endpoints for homepage stats")
public class PublicController {

    private final UserRepository userRepository;
    private final PlaceRepository placeRepository;
    private final QueueRepository queueRepository;

    @GetMapping("/stats")
    @Operation(summary = "Get public statistics", description = "Returns total users, places, and queues served for the homepage.")
    @ApiResponse(responseCode = "200", description = "Statistics map")
    public ResponseEntity<Map<String, Object>> getPublicStats() {
        log.info("Fetching public statistics");
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalPlaces", placeRepository.count());
        // For "queues served" we can use total completed tokens across all queues.
        long totalQueuesServed = queueRepository.findAll().stream()
                .flatMap(q -> q.getTokens().stream())
                .filter(t -> "COMPLETED".equals(t.getStatus()))
                .count();
        stats.put("totalQueuesServed", totalQueuesServed);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/live-stats")
    @Operation(summary = "Get live queue statistics", description = "Returns number of active queues and average wait time.")
    @ApiResponse(responseCode = "200", description = "Live stats")
    public ResponseEntity<Map<String, Object>> getLiveStats() {
        log.info("Fetching live queue statistics");
        List<Queue> allQueues = queueRepository.findAll();
        long activeQueues = allQueues.stream().filter(Queue::getIsActive).count();

        double avgWaitTime = allQueues.stream()
                .filter(Queue::getIsActive)
                .mapToInt(Queue::getEstimatedWaitTime)
                .average()
                .orElse(0.0);

        Map<String, Object> stats = new HashMap<>();
        stats.put("activeQueues", activeQueues);
        stats.put("averageWaitTime", Math.round(avgWaitTime * 10) / 10.0); // one decimal
        return ResponseEntity.ok(stats);
    }
}