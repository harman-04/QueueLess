package com.queueless.backend.service;

import com.queueless.backend.enums.TokenStatus;
import com.queueless.backend.model.Queue;
import com.queueless.backend.model.QueueHourlyStats;
import com.queueless.backend.repository.QueueHourlyStatsRepository;
import com.queueless.backend.repository.QueueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProviderAnalyticsService {

    private final QueueRepository queueRepository;
    private final QueueHourlyStatsRepository statsRepository;

    public Map<String, Object> getTokensOverTime(String providerId, int days) {
        log.info("Fetching token volume over last {} days for provider: {}", days, providerId);
        LocalDateTime start = LocalDateTime.now().minusDays(days).withHour(0).withMinute(0).withSecond(0);

        List<Queue> queues = queueRepository.findByProviderId(providerId);

        Map<LocalDate, Long> dailyCounts = queues.stream()
                .flatMap(q -> q.getTokens().stream())
                .filter(t -> TokenStatus.COMPLETED.toString().equals(t.getStatus()))
                .filter(t -> t.getCompletedAt() != null && t.getCompletedAt().isAfter(start))
                .collect(Collectors.groupingBy(
                        t -> t.getCompletedAt().toLocalDate(),
                        Collectors.counting()
                ));

        List<String> dates = new ArrayList<>();
        List<Long> counts = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            dates.add(date.toString());
            counts.add(dailyCounts.getOrDefault(date, 0L));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("dates", dates);
        result.put("counts", counts);
        return result;
    }

    public Map<Integer, Double> getBusiestHours(String providerId) {
        log.info("Fetching busiest hours for provider: {}", providerId);
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        List<Queue> queues = queueRepository.findByProviderId(providerId);
        List<String> queueIds = queues.stream().map(Queue::getId).toList();

        List<QueueHourlyStats> stats = statsRepository.findByQueueIdInAndHourBetween(queueIds, thirtyDaysAgo, LocalDateTime.now());

        Map<Integer, Double> avgByHour = stats.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getHour().getHour(),
                        Collectors.averagingInt(QueueHourlyStats::getWaitingCount)
                ));

        Map<Integer, Double> result = new LinkedHashMap<>();
        for (int hour = 0; hour < 24; hour++) {
            result.put(hour, avgByHour.getOrDefault(hour, 0.0));
        }
        return result;
    }

    public Map<String, Object> getAverageWaitTimeTrend(String providerId, int days) {
        log.info("Fetching average wait time trend over last {} days for provider: {}", days, providerId);
        LocalDateTime start = LocalDateTime.now().minusDays(days).withHour(0).withMinute(0).withSecond(0);

        List<Queue> queues = queueRepository.findByProviderId(providerId);

        Map<LocalDate, List<Long>> dailyWaitTimes = new HashMap<>();
        queues.stream()
                .flatMap(q -> q.getTokens().stream())
                .filter(t -> TokenStatus.COMPLETED.toString().equals(t.getStatus()))
                .filter(t -> t.getServedAt() != null && t.getIssuedAt() != null)
                .filter(t -> t.getServedAt().isAfter(start))
                .forEach(t -> {
                    LocalDate date = t.getServedAt().toLocalDate();
                    long waitMinutes = java.time.Duration.between(t.getIssuedAt(), t.getServedAt()).toMinutes();
                    dailyWaitTimes.computeIfAbsent(date, k -> new ArrayList<>()).add(waitMinutes);
                });

        List<String> dates = new ArrayList<>();
        List<Double> averages = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            dates.add(date.toString());
            List<Long> waits = dailyWaitTimes.get(date);
            double avg = (waits == null || waits.isEmpty()) ? 0.0 : waits.stream().mapToLong(v -> v).average().orElse(0.0);
            averages.add(avg);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("dates", dates);
        result.put("averages", averages);
        return result;
    }
}