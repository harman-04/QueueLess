package com.queueless.backend.service;

import com.queueless.backend.dto.*;
import com.queueless.backend.enums.Role;
import com.queueless.backend.enums.TokenStatus;
import com.queueless.backend.exception.AccessDeniedException;
import com.queueless.backend.exception.ResourceNotFoundException;
import com.queueless.backend.model.*;
import com.queueless.backend.model.Queue;
import com.queueless.backend.repository.*;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final PlaceRepository placeRepository;
    private final QueueRepository queueRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final QueueHourlyStatsRepository statsRepository;
    private final FeedbackRepository feedbackRepository;
    private final TokenRepository tokenRepository;
    private final PlaceService placeService;
    private final PasswordResetService passwordResetService;
    private final PasswordResetTokenService passwordResetTokenService;

    public Map<String, Object> getDashboardStats(String adminId) {
        log.info("Fetching dashboard stats for admin: {}", adminId);
        Map<String, Object> stats = new HashMap<>();

        // Get places owned by this admin
        List<Place> adminPlaces = placeRepository.findByAdminId(adminId);
        List<String> placeIds = adminPlaces.stream().map(Place::getId).toList();
        log.debug("Found {} places for admin", adminPlaces.size());

        // Get queues for these places
        List<Queue> queues = queueRepository.findByPlaceIdIn(placeIds);
        log.debug("Found {} queues for admin's places", queues.size());

        // Calculate statistics
        stats.put("totalPlaces", adminPlaces.size());
        stats.put("totalQueues", queues.size());
        stats.put("activeQueues", queues.stream().filter(Queue::getIsActive).count());

        // Calculate total tokens served today
        long tokensServedToday = queues.stream()
                .flatMap(queue -> queue.getTokens().stream())
                .filter(token -> "COMPLETED".equals(token.getStatus()))
                .filter(token -> token.getCompletedAt() != null &&
                        token.getCompletedAt().toLocalDate().equals(LocalDate.now()))
                .count();
        stats.put("tokensServedToday", tokensServedToday);
        log.debug("Tokens served today: {}", tokensServedToday);

        // Calculate active users in queues
        long activeUsers = queues.stream()
                .flatMap(queue -> queue.getTokens().stream())
                .filter(token -> "WAITING".equals(token.getStatus()) || "IN_SERVICE".equals(token.getStatus()))
                .map(token -> token.getUserId())
                .distinct()
                .count();
        stats.put("activeUsers", activeUsers);
        log.debug("Active users: {}", activeUsers);

        // Get provider count for this admin
        long providerCount = userRepository.findAll().stream()
                .filter(user -> adminId.equals(user.getAdminId()) && "PROVIDER".equals(user.getRole().name()))
                .count();
        stats.put("providerCount", providerCount);
        log.debug("Provider count: {}", providerCount);

        // Get recent activity
        stats.put("recentActivity", getRecentActivity(queues));
        log.info("Dashboard stats prepared for admin: {}", adminId);

        return stats;
    }


    private List<Map<String, Object>> getRecentActivity(List<Queue> queues) {
        return queues.stream()
                .flatMap(queue -> queue.getTokens().stream())
                .filter(token -> token.getCompletedAt() != null &&
                        token.getCompletedAt().isAfter(LocalDateTime.now().minusHours(24)))
                .sorted((t1, t2) -> t2.getCompletedAt().compareTo(t1.getCompletedAt()))
                .limit(10)
                .map(token -> {
                    Map<String, Object> activity = new HashMap<>();
                    activity.put("tokenId", token.getTokenId());
                    activity.put("completedAt", token.getCompletedAt());
                    activity.put("queueId", queues.stream()
                            .filter(q -> q.getTokens().contains(token))
                            .findFirst()
                            .map(Queue::getId)
                            .orElse("Unknown"));
                    return activity;
                })
                .collect(Collectors.toList());
    }

    public List<AdminQueueDTO> getAdminQueuesWithDetails(String adminId) {
        log.info("Fetching enhanced queue details for admin: {}", adminId);
        List<Place> adminPlaces = placeRepository.findByAdminId(adminId);
        List<String> placeIds = adminPlaces.stream().map(Place::getId).toList();

        Map<String, String> placeNames = adminPlaces.stream()
                .collect(Collectors.toMap(Place::getId, Place::getName));

        List<Queue> queues = queueRepository.findByPlaceIdIn(placeIds);
        log.debug("Found {} queues for admin's places", queues.size());

        List<String> providerIds = queues.stream()
                .map(Queue::getProviderId)
                .distinct()
                .collect(Collectors.toList());

        Map<String, String> providerNames = userRepository.findAllById(providerIds).stream()
                .collect(Collectors.toMap(User::getId, User::getName));

        return queues.stream().map(queue -> {
            AdminQueueDTO dto = new AdminQueueDTO();
            dto.setId(queue.getId());
            dto.setServiceName(queue.getServiceName());
            dto.setPlaceName(placeNames.get(queue.getPlaceId()));
            dto.setProviderName(providerNames.get(queue.getProviderId()));
            dto.setIsActive(queue.getIsActive());
            dto.setWaitingTokens((int) queue.getTokens().stream()
                    .filter(t -> "WAITING".equals(t.getStatus())).count());
            dto.setInServiceTokens((int) queue.getTokens().stream()
                    .filter(t -> "IN_SERVICE".equals(t.getStatus())).count());
            dto.setCompletedTokens((int) queue.getTokens().stream()
                    .filter(t -> "COMPLETED".equals(t.getStatus())).count());
            dto.setEstimatedWaitTime(queue.getEstimatedWaitTime());
            return dto;
        }).collect(Collectors.toList());
    }

    public List<Payment> getAdminPaymentHistory(String adminId) {
        log.info("Fetching enhanced payment history for admin: {}", adminId);
        Optional<User> adminUser = userRepository.findById(adminId);
        if (adminUser.isEmpty()) {
            log.warn("Admin user not found with ID: {}", adminId);
            return List.of();
        }

        String adminEmail = adminUser.get().getEmail();

        // Get payments made by admin for providers
        List<Payment> providerPayments = paymentRepository.findByCreatedByAdminId(adminId);
        log.debug("Found {} provider payments", providerPayments.size());

        // Get payments made by admin for themselves
        List<Payment> adminPayments = paymentRepository.findByCreatedForEmail(adminEmail);
        log.debug("Found {} admin self-payments", adminPayments.size());

        // Combine and sort by date
        List<Payment> allPayments = new ArrayList<>();
        allPayments.addAll(providerPayments);
        allPayments.addAll(adminPayments);

        List<Payment> sorted = allPayments.stream()
                .sorted((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()))
                .collect(Collectors.toList());

        log.info("Total {} payment records found for admin", sorted.size());
        return sorted;
    }

    public Map<String, Object> getTokensOverTime(String adminId, int days) {
        log.info("Fetching token volume over last {} days for admin: {}", days, adminId);
        LocalDateTime start = LocalDateTime.now().minusDays(days).withHour(0).withMinute(0).withSecond(0);

        // Get all place IDs under this admin
        List<Place> adminPlaces = placeRepository.findByAdminId(adminId);
        List<String> placeIds = adminPlaces.stream().map(Place::getId).toList();

        // Get all queues for those places
        List<Queue> queues = queueRepository.findByPlaceIdIn(placeIds);

        // Collect all completed tokens
        Map<LocalDate, Long> dailyCounts = queues.stream()
                .flatMap(q -> q.getTokens().stream())
                .filter(t -> TokenStatus.COMPLETED.toString().equals(t.getStatus()))
                .filter(t -> t.getCompletedAt() != null && t.getCompletedAt().isAfter(start))
                .collect(Collectors.groupingBy(
                        t -> t.getCompletedAt().toLocalDate(),
                        Collectors.counting()
                ));

        // Build ordered map of dates and counts
        List<String> dates = new ArrayList<>();
        List<Long> counts = new ArrayList<>();
        for (int i = days-1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            dates.add(date.toString());
            counts.add(dailyCounts.getOrDefault(date, 0L));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("dates", dates);
        result.put("counts", counts);
        return result;
    }

    public Map<Integer, Double> getBusiestHours(String adminId) {
        log.info("Fetching busiest hours for admin: {}", adminId);
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        // Get all place IDs under this admin
        List<Place> adminPlaces = placeRepository.findByAdminId(adminId);
        List<String> placeIds = adminPlaces.stream().map(Place::getId).toList();

        // Get all queues for those places
        List<Queue> queues = queueRepository.findByPlaceIdIn(placeIds);
        List<String> queueIds = queues.stream().map(Queue::getId).toList();

        // Fetch hourly stats for all those queues
        List<QueueHourlyStats> stats = statsRepository.findByQueueIdInAndHourBetween(queueIds, thirtyDaysAgo, LocalDateTime.now());

        // Group by hour and calculate average waiting count
        Map<Integer, Double> avgByHour = stats.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getHour().getHour(),
                        Collectors.averagingInt(QueueHourlyStats::getWaitingCount)
                ));

        // Fill missing hours (0-23) with 0.0
        Map<Integer, Double> result = new LinkedHashMap<>();
        for (int hour = 0; hour < 24; hour++) {
            result.put(hour, avgByHour.getOrDefault(hour, 0.0));
        }
        return result;
    }

    private Double getAverageRatingForProvider(String providerId) {
        List<Feedback> feedbacks = feedbackRepository.findByProviderId(providerId);
        if (feedbacks.isEmpty()) return 0.0;
        return feedbacks.stream()
                .filter(f -> f.getRating() != null)
                .mapToDouble(Feedback::getRating)
                .average()
                .orElse(0.0);
    }

    private Double getCancellationRateForProvider(String providerId) {
        List<Queue> queues = queueRepository.findByProviderId(providerId);
        long totalCancelled = queues.stream()
                .flatMap(q -> q.getTokens().stream())
                .filter(t -> "CANCELLED".equals(t.getStatus()))
                .count();
        long totalServed = queues.stream()
                .flatMap(q -> q.getTokens().stream())
                .filter(t -> "COMPLETED".equals(t.getStatus()))
                .count();
        long total = totalCancelled + totalServed;
        if (total == 0) return 0.0;
        return (double) totalCancelled / total * 100;
    }

    public List<ProviderPerformanceDTO> getProvidersWithQueues(String adminId) {
        log.info("Fetching providers with queues for admin: {}", adminId);
        List<User> providers = userRepository.findAll().stream()
                .filter(user -> adminId.equals(user.getAdminId()) && "PROVIDER".equals(user.getRole().name()))
                .collect(Collectors.toList());

        return providers.stream().map(provider -> {
            List<Queue> providerQueues = queueRepository.findByProviderId(provider.getId());
            int totalQueues = providerQueues.size();
            int activeQueues = (int) providerQueues.stream().filter(Queue::getIsActive).count();
            long tokensServedToday = providerQueues.stream()
                    .flatMap(queue -> queue.getTokens().stream())
                    .filter(token -> "COMPLETED".equals(token.getStatus()))
                    .filter(token -> token.getCompletedAt() != null &&
                            token.getCompletedAt().toLocalDate().equals(LocalDate.now()))
                    .count();
            double avgRating = getAverageRatingForProvider(provider.getId());
            double cancellationRate = getCancellationRateForProvider(provider.getId());

            return new ProviderPerformanceDTO(provider, totalQueues, activeQueues,
                    tokensServedToday, avgRating, cancellationRate);
        }).collect(Collectors.toList());
    }

    public List<PlaceWithQueueDTO> getPlacesWithQueueStats(String adminId) {
        log.info("Fetching places with queue stats for admin: {}", adminId);
        List<Place> adminPlaces = placeRepository.findByAdminId(adminId);

        return adminPlaces.stream().map(place -> {
            List<Queue> queues = queueRepository.findByPlaceId(place.getId());
            int waiting = 0;
            int inService = 0;
            for (Queue queue : queues) {
                waiting += (int) queue.getTokens().stream()
                        .filter(t -> "WAITING".equals(t.getStatus())).count();
                inService += (int) queue.getTokens().stream()
                        .filter(t -> "IN_SERVICE".equals(t.getStatus())).count();
            }
            return new PlaceWithQueueDTO(place, waiting, inService);
        }).collect(Collectors.toList());
    }

    public AdminReportDTO getAdminReport(String adminId) {
        log.info("Generating admin report for admin: {}", adminId);
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        List<Place> adminPlaces = placeRepository.findByAdminId(adminId);
        List<AdminReportDTO.PlaceSummaryDTO> placeSummaries = new ArrayList<>();
        AdminReportDTO.GlobalSummaryDTO.GlobalSummaryDTOBuilder globalBuilder = AdminReportDTO.GlobalSummaryDTO.builder();

        long totalServedToday = 0;
        long totalServedAllTime = 0;
        double totalRatingSum = 0;
        int ratingCount = 0;
        double totalWaitTimeSum = 0;
        int waitTimeCount = 0;

        for (Place place : adminPlaces) {
            List<Queue> queues = queueRepository.findByPlaceId(place.getId());
            int totalQueues = queues.size();
            int activeQueues = (int) queues.stream().filter(Queue::getIsActive).count();

            long servedToday = 0;
            long servedAllTime = 0;
            double waitTimeSum = 0;
            int waitTimeTokens = 0;
            long activeTokens = 0;

            for (Queue queue : queues) {
                // Tokens served today
                servedToday += queue.getTokens().stream()
                        .filter(t -> "COMPLETED".equals(t.getStatus()))
                        .filter(t -> t.getCompletedAt() != null && t.getCompletedAt().toLocalDate().equals(LocalDate.now()))
                        .count();
                // Tokens served all time
                servedAllTime += queue.getTokens().stream()
                        .filter(t -> "COMPLETED".equals(t.getStatus()))
                        .count();
                // Wait time average per token (only for completed)
                for (QueueToken token : queue.getTokens()) {
                    if ("COMPLETED".equals(token.getStatus()) && token.getServedAt() != null && token.getIssuedAt() != null) {
                        long wait = Duration.between(token.getIssuedAt(), token.getServedAt()).toMinutes();
                        waitTimeSum += wait;
                        waitTimeTokens++;
                    }
                }
                // Active tokens
                activeTokens += queue.getTokens().stream()
                        .filter(t -> "WAITING".equals(t.getStatus()) || "IN_SERVICE".equals(t.getStatus()))
                        .count();
            }

            // Average rating for this place
            List<Feedback> feedbacks = feedbackRepository.findByPlaceId(place.getId());
            double avgRating = feedbacks.stream()
                    .filter(f -> f.getRating() != null)
                    .mapToDouble(Feedback::getRating)
                    .average()
                    .orElse(0.0);
            totalRatingSum += avgRating * feedbacks.size(); // weighted sum
            ratingCount += feedbacks.size();

            placeSummaries.add(AdminReportDTO.PlaceSummaryDTO.builder()
                    .placeId(place.getId())
                    .placeName(place.getName())
                    .totalQueues(totalQueues)
                    .activeQueues(activeQueues)
                    .tokensServedToday(servedToday)
                    .tokensServedTotal(servedAllTime)
                    .averageWaitTime(waitTimeTokens > 0 ? (double) waitTimeSum / waitTimeTokens : 0)
                    .averageRating(avgRating)
                    .activeTokens(activeTokens)
                    .build());

            totalServedToday += servedToday;
            totalServedAllTime += servedAllTime;
            totalWaitTimeSum += waitTimeSum;
            waitTimeCount += waitTimeTokens;
        }

        AdminReportDTO.GlobalSummaryDTO globalSummary = AdminReportDTO.GlobalSummaryDTO.builder()
                .totalPlaces(adminPlaces.size())
                .totalQueues(placeSummaries.stream().mapToInt(AdminReportDTO.PlaceSummaryDTO::getTotalQueues).sum())
                .totalTokensServedToday(totalServedToday)
                .totalTokensServedAllTime(totalServedAllTime)
                .averageRatingOverall(ratingCount > 0 ? totalRatingSum / ratingCount : 0)
                .averageWaitTimeOverall(waitTimeCount > 0 ? totalWaitTimeSum / waitTimeCount : 0)
                .build();

        return AdminReportDTO.builder()
                .adminName(admin.getName())
                .adminEmail(admin.getEmail())
                .generatedAt(LocalDateTime.now())
                .places(placeSummaries)
                .summary(globalSummary)
                .build();
    }

    public List<PaymentHistoryDTO> getProviderTokensHistory(String adminId) {
        log.info("Fetching provider tokens for admin: {}", adminId);
        List<Token> providerTokens = tokenRepository.findByCreatedByAdminIdAndRole(adminId, Role.PROVIDER);

        return providerTokens.stream()
                .map(token -> PaymentHistoryDTO.builder()
                        .id(token.getId())
                        .description("Provider token for " + token.getCreatedForEmail())
                        .amount(0) // no payment record, amount unknown
                        .role(Role.PROVIDER.name())
                        .status("Completed")
                        .createdAt(token.getExpiryDate()) // use expiry as creation? Better to use a creation date field if exists.
                        .reference(token.getTokenValue())
                        .isPaid(true)
                        .build())
                .collect(Collectors.toList());
    }

    public List<PaymentHistoryDTO> getEnhancedPaymentHistory(String adminId) {
        List<PaymentHistoryDTO> result = new ArrayList<>();

        // 1. Payments made by admin for providers
        List<Payment> providerPayments = paymentRepository.findByCreatedByAdminId(adminId);
        for (Payment p : providerPayments) {
            result.add(PaymentHistoryDTO.builder()
                    .id(p.getId())
                    .description("Provider token for " + p.getCreatedForEmail())
                    .amount(p.getAmount())
                    .role(p.getRole().name())
                    .status(p.isPaid() ? "Completed" : "Pending")
                    .createdAt(p.getCreatedAt())
                    .reference(p.getRazorpayOrderId())
                    .isPaid(p.isPaid())
                    .build());
        }

        // 2. Payments for admin's own tokens
        User admin = userRepository.findById(adminId).orElse(null);
        if (admin != null) {
            List<Payment> adminPayments = paymentRepository.findByCreatedForEmail(admin.getEmail());
            for (Payment p : adminPayments) {
                result.add(PaymentHistoryDTO.builder()
                        .id(p.getId())
                        .description("Admin token")
                        .amount(p.getAmount())
                        .role(p.getRole().name())
                        .status(p.isPaid() ? "Completed" : "Pending")
                        .createdAt(p.getCreatedAt())
                        .reference(p.getRazorpayOrderId())
                        .isPaid(p.isPaid())
                        .build());
            }
        }

        // 3. Provider tokens that have no payment record (legacy)
        List<Token> providerTokens = tokenRepository.findByCreatedByAdminIdAndRole(adminId, Role.PROVIDER);
        Set<String> paidEmails = providerPayments.stream()
                .map(Payment::getCreatedForEmail)
                .collect(Collectors.toSet());
        for (Token t : providerTokens) {
            if (paidEmails.contains(t.getCreatedForEmail())) continue;
            result.add(PaymentHistoryDTO.builder()
                    .id(t.getId())
                    .description("Provider token for " + (t.getProviderEmail() != null ? t.getProviderEmail() : t.getCreatedForEmail()))
                    .amount(0) // unknown
                    .role(Role.PROVIDER.name())
                    .status("Completed")
                    .createdAt(t.getCreatedAt() != null ? t.getCreatedAt() : t.getExpiryDate()) // use createdAt if available
                    .reference(t.getTokenValue())
                    .isPaid(true)
                    .build());
        }

        // Sort by date descending
        result.sort((a, b) -> {
            if (a.getCreatedAt() == null) return 1;
            if (b.getCreatedAt() == null) return -1;
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });
        return result;
    }

    public ProviderDetailsDTO getProviderById(String providerId, String requestingAdminId) {
        log.info("Fetching provider details for providerId: {} by admin: {}", providerId, requestingAdminId);

        User provider = userRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found with id: " + providerId));

        // Ensure the requesting admin owns this provider
        if (!requestingAdminId.equals(provider.getAdminId())) {
            log.warn("Admin {} attempted to access provider {} not under their control", requestingAdminId, providerId);
            throw new AccessDeniedException("You can only access providers under your administration");
        }

        // Build statistics
        List<Queue> queues = queueRepository.findByProviderId(providerId);
        int totalQueues = queues.size();
        int activeQueues = (int) queues.stream().filter(Queue::getIsActive).count();

        long tokensServedToday = queues.stream()
                .flatMap(q -> q.getTokens().stream())
                .filter(t -> "COMPLETED".equals(t.getStatus()))
                .filter(t -> t.getCompletedAt() != null &&
                        t.getCompletedAt().toLocalDate().equals(LocalDate.now()))
                .count();

        long tokensServedTotal = queues.stream()
                .flatMap(q -> q.getTokens().stream())
                .filter(t -> "COMPLETED".equals(t.getStatus()))
                .count();

        double avgRating = getAverageRatingForProvider(providerId);
        double cancellationRate = getCancellationRateForProvider(providerId);

        // Get managed places with details
        List<Place> managedPlaces = placeService.getPlacesByIds(
                provider.getManagedPlaceIds() != null ? provider.getManagedPlaceIds() : List.of());

        return ProviderDetailsDTO.builder()
                .id(provider.getId())
                .name(provider.getName())
                .email(provider.getEmail())
                .phoneNumber(provider.getPhoneNumber())
                .profileImageUrl(provider.getProfileImageUrl())
                .isVerified(provider.getIsVerified())
                .isActive(provider.getIsActive() != null ? provider.getIsActive() : true) // handle null from old users
                .adminId(provider.getAdminId())
                .managedPlaceIds(provider.getManagedPlaceIds())
                .managedPlaces(managedPlaces.stream().map(PlaceDTO::fromEntity).collect(Collectors.toList()))
                .totalQueues(totalQueues)
                .activeQueues(activeQueues)
                .tokensServedToday(tokensServedToday)
                .tokensServedTotal(tokensServedTotal)
                .averageRating(avgRating)
                .cancellationRate(cancellationRate)
                .build();
    }

    public ProviderDetailsDTO updateProvider(String providerId, ProviderUpdateRequest request, String requestingAdminId) {
        log.info("Updating provider {} by admin {}", providerId, requestingAdminId);

        User provider = userRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found with id: " + providerId));

        // Verify admin ownership
        if (!requestingAdminId.equals(provider.getAdminId())) {
            log.warn("Admin {} attempted to update provider {} not under their control", requestingAdminId, providerId);
            throw new AccessDeniedException("You can only update providers under your administration");
        }

        // Update fields if provided
        if (request.getName() != null) {
            provider.setName(request.getName());
        }
        if (request.getEmail() != null) {
            provider.setEmail(request.getEmail());
        }
        if (request.getPhoneNumber() != null) {
            provider.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getManagedPlaceIds() != null) {
            // Validate that all place IDs belong to this admin
            List<Place> adminPlaces = placeRepository.findByAdminId(requestingAdminId);
            Set<String> validPlaceIds = adminPlaces.stream().map(Place::getId).collect(Collectors.toSet());
            for (String placeId : request.getManagedPlaceIds()) {
                if (!validPlaceIds.contains(placeId)) {
                    throw new IllegalArgumentException("Place " + placeId + " does not belong to your admin account");
                }
            }
            provider.setManagedPlaceIds(request.getManagedPlaceIds());
        }
        if (request.getIsActive() != null) {
            provider.setIsActive(request.getIsActive());
        }

        userRepository.save(provider);

        // Return updated details (reuse getProviderById logic)
        return getProviderById(providerId, requestingAdminId);
    }

    public ProviderDetailsDTO toggleProviderStatus(String providerId, boolean active, String requestingAdminId) {
        log.info("Toggling provider {} status to {} by admin {}", providerId, active, requestingAdminId);

        User provider = userRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found with id: " + providerId));

        if (!requestingAdminId.equals(provider.getAdminId())) {
            log.warn("Admin {} attempted to toggle provider {} not under their control", requestingAdminId, providerId);
            throw new AccessDeniedException("You can only update providers under your administration");
        }

        provider.setIsActive(active);
        userRepository.save(provider);

        // Return updated details
        return getProviderById(providerId, requestingAdminId);
    }

//    public void resetProviderPassword(String providerId, String requestingAdminId) throws MessagingException {
//        log.info("Admin {} requested password reset for provider {}", requestingAdminId, providerId);
//
//        User provider = userRepository.findById(providerId)
//                .orElseThrow(() -> new ResourceNotFoundException("Provider not found with id: " + providerId));
//
//        if (!requestingAdminId.equals(provider.getAdminId())) {
//            log.warn("Admin {} attempted to reset password for provider {} not under their control", requestingAdminId, providerId);
//            throw new AccessDeniedException("You can only reset passwords for providers under your administration");
//        }
//
//        ForgotPasswordRequest request = new ForgotPasswordRequest(provider.getEmail());
//        passwordResetService.sendOtp(request);
//    }

    public void resetProviderPassword(String providerId, String requestingAdminId) {
        log.info("Admin {} requested password reset for provider {}", requestingAdminId, providerId);

        User provider = userRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found with id: " + providerId));

        if (!requestingAdminId.equals(provider.getAdminId())) {
            log.warn("Admin {} attempted to reset password for provider {} not under their control", requestingAdminId, providerId);
            throw new AccessDeniedException("You can only reset passwords for providers under your administration");
        }

        // Generate and send token
        passwordResetTokenService.createAndSendToken(provider);
    }

}