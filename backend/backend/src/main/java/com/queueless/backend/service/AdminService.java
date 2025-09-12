// Enhanced AdminService.java
package com.queueless.backend.service;

import com.queueless.backend.dto.AdminQueueDTO;
import com.queueless.backend.model.Payment;
import com.queueless.backend.model.Place;
import com.queueless.backend.model.Queue;
import com.queueless.backend.model.User;
import com.queueless.backend.repository.PaymentRepository;
import com.queueless.backend.repository.PlaceRepository;
import com.queueless.backend.repository.QueueRepository;
import com.queueless.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final PlaceRepository placeRepository;
    private final QueueRepository queueRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;

    public Map<String, Object> getDashboardStats(String adminId) {
        Map<String, Object> stats = new HashMap<>();

        // Get places owned by this admin
        List<Place> adminPlaces = placeRepository.findByAdminId(adminId);
        List<String> placeIds = adminPlaces.stream().map(Place::getId).toList();

        // Get queues for these places
        List<Queue> queues = queueRepository.findByPlaceIdIn(placeIds);

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

        // Calculate active users in queues
        long activeUsers = queues.stream()
                .flatMap(queue -> queue.getTokens().stream())
                .filter(token -> "WAITING".equals(token.getStatus()) || "IN_SERVICE".equals(token.getStatus()))
                .map(token -> token.getUserId())
                .distinct()
                .count();
        stats.put("activeUsers", activeUsers);

        // Get provider count for this admin
        long providerCount = userRepository.findAll().stream()
                .filter(user -> adminId.equals(user.getAdminId()) && "PROVIDER".equals(user.getRole().name()))
                .count();
        stats.put("providerCount", providerCount);

        // Get recent activity
        stats.put("recentActivity", getRecentActivity(queues));

        return stats;
    }

    public List<Map<String, Object>> getProvidersWithQueues(String adminId) {
        // Get all providers for this admin
        List<User> providers = userRepository.findAll().stream()
                .filter(user -> adminId.equals(user.getAdminId()) && "PROVIDER".equals(user.getRole().name()))
                .collect(Collectors.toList());

        return providers.stream().map(provider -> {
            Map<String, Object> providerData = new HashMap<>();
            providerData.put("provider", provider);

            // Get queues managed by this provider
            List<Queue> providerQueues = queueRepository.findByProviderId(provider.getId());
            providerData.put("queues", providerQueues);

            // Calculate provider stats
            Map<String, Object> providerStats = new HashMap<>();
            providerStats.put("totalQueues", providerQueues.size());
            providerStats.put("activeQueues", providerQueues.stream().filter(Queue::getIsActive).count());
            providerStats.put("tokensServedToday", providerQueues.stream()
                    .flatMap(queue -> queue.getTokens().stream())
                    .filter(token -> "COMPLETED".equals(token.getStatus()))
                    .filter(token -> token.getCompletedAt() != null &&
                            token.getCompletedAt().toLocalDate().equals(LocalDate.now()))
                    .count());

            providerData.put("stats", providerStats);
            return providerData;
        }).collect(Collectors.toList());
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


    // Update AdminService.java to include methods for getting enhanced data
    public List<AdminQueueDTO> getAdminQueuesWithDetails(String adminId) {
        List<Place> adminPlaces = placeRepository.findByAdminId(adminId);
        List<String> placeIds = adminPlaces.stream().map(Place::getId).toList();

        // Create a map of place IDs to names
        Map<String, String> placeNames = adminPlaces.stream()
                .collect(Collectors.toMap(Place::getId, Place::getName));

        // Get all queues for these places
        List<Queue> queues = queueRepository.findByPlaceIdIn(placeIds);

        // Get provider details
        List<String> providerIds = queues.stream()
                .map(Queue::getProviderId)
                .distinct()
                .collect(Collectors.toList());

        Map<String, String> providerNames = userRepository.findAllById(providerIds).stream()
                .collect(Collectors.toMap(User::getId, User::getName));

        // Convert to DTO
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
        Optional<User> adminUser = userRepository.findById(adminId);
        if (adminUser.isEmpty()) return List.of();

        String adminEmail = adminUser.get().getEmail();

        // Get payments made by admin for providers
        List<Payment> providerPayments = paymentRepository.findByCreatedByAdminId(adminId);

        // Get payments made by admin for themselves
        List<Payment> adminPayments = paymentRepository.findByCreatedForEmail(adminEmail);

        // Combine and sort by date
        List<Payment> allPayments = new ArrayList<>();
        allPayments.addAll(providerPayments);
        allPayments.addAll(adminPayments);

        return allPayments.stream()
                .sorted((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()))
                .collect(Collectors.toList());
    }
}