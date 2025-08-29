//package com.queueless.backend.service;
//
//import com.queueless.backend.dto.AnalyticsResponse;
//import com.queueless.backend.model.Token;
//import com.queueless.backend.repository.TokenRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//import java.util.*;
//
//@Service
//@RequiredArgsConstructor
//public class AnalyticsService {
//
//    private final TokenRepository tokenRepository;
//
//    public AnalyticsResponse getQueueAnalytics(String queueId) {
//        List<Token> tokens = tokenRepository.findByQueueIdOrderByTokenNumberAsc(queueId);
//
//        Map<String, Double> averageWaitTimesByHour = new HashMap<>();
//        Map<String, Long> loadOverTime = new TreeMap<>();
//
//        int[] hourCount = new int[24];
//        double[] totalWaitPerHour = new double[24];
//        int[] dayCount = new int[7];
//
//        for (Token token : tokens) {
//            if (token.getIssuedAt() == null || token.getEstimatedServingTime() == null) continue;
//
//            LocalDateTime issued = token.getIssuedAt();
//            LocalDateTime est = token.getEstimatedServingTime();
//
//            int hour = issued.getHour();
//            int day = issued.getDayOfWeek().getValue() - 1;
//
//            long waitMinutes = java.time.Duration.between(issued, est).toMinutes();
//            totalWaitPerHour[hour] += waitMinutes;
//            hourCount[hour]++;
//            dayCount[day]++;
//
//            String timestampKey = issued.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH"));
//            loadOverTime.put(timestampKey, loadOverTime.getOrDefault(timestampKey, 0L) + 1);
//        }
//
//        for (int h = 0; h < 24; h++) {
//            if (hourCount[h] > 0) {
//                averageWaitTimesByHour.put(h + ":00", totalWaitPerHour[h] / hourCount[h]);
//            }
//        }
//
//        int peakHour = Arrays.stream(hourCount).boxed().max(Integer::compare).orElse(0);
//        int peakDay = Arrays.stream(dayCount).boxed().max(Integer::compare).orElse(0);
//
//        AnalyticsResponse res = new AnalyticsResponse();
//        res.setQueueId(queueId);
//        res.setAverageWaitTimesByHour(averageWaitTimesByHour);
//        res.setLoadOverTime(loadOverTime);
//        res.setPeakHourOfDay(peakHour);
//        res.setPeakDayOfWeek(peakDay);
//        return res;
//    }
//}
