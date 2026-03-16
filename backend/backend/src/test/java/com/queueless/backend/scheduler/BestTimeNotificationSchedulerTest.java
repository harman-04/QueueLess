package com.queueless.backend.scheduler;

import com.queueless.backend.model.NotificationPreference;
import com.queueless.backend.model.Queue;
import com.queueless.backend.model.QueueToken;
import com.queueless.backend.model.User;
import com.queueless.backend.repository.NotificationPreferenceRepository;
import com.queueless.backend.repository.QueueRepository;
import com.queueless.backend.repository.UserRepository;
import com.queueless.backend.service.FcmService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BestTimeNotificationSchedulerTest {

    @Mock
    private QueueRepository queueRepository;

    @Mock
    private NotificationPreferenceRepository preferenceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FcmService fcmService;

    @InjectMocks
    private BestTimeNotificationScheduler scheduler;

    private final String queueId = "queue123";
    private final String userId1 = "user1";
    private final String userId2 = "user2";

    private Queue createQueueWithWaitingCount(long waitingCount) {
        Queue queue = new Queue("provider", "Test Service", "place123", "service123");
        queue.setId(queueId);
        queue.setIsActive(true);
        for (int i = 0; i < waitingCount; i++) {
            QueueToken token = new QueueToken("T" + i, "user" + i, "WAITING", LocalDateTime.now());
            queue.getTokens().add(token);
        }
        return queue;
    }

    private NotificationPreference createPreference(String userId, boolean notifyOnBestTime, boolean enabled) {
        NotificationPreference pref = new NotificationPreference();
        pref.setUserId(userId);
        pref.setQueueId(queueId);
        pref.setNotifyOnBestTime(notifyOnBestTime);
        pref.setEnabled(enabled);
        pref.setLastBestTimeNotificationSent(null);
        return pref;
    }

    private User createUser(String userId, List<String> fcmTokens) {
        return User.builder()
                .id(userId)
                .fcmTokens(fcmTokens)
                .build();
    }

    @Test
    void shouldNotNotifyWhenQueueIsAboveThreshold() {
        Queue queue = createQueueWithWaitingCount(5); // > threshold
        when(queueRepository.findByIsActive(true)).thenReturn(List.of(queue));

        scheduler.checkBestTimeNotifications();

        verifyNoInteractions(preferenceRepository, userRepository, fcmService);
    }

    @Test
    void shouldNotNotifyWhenNoPreferencesForQueue() {
        Queue queue = createQueueWithWaitingCount(2); // below threshold
        when(queueRepository.findByIsActive(true)).thenReturn(List.of(queue));
        when(preferenceRepository.findByQueueId(queueId)).thenReturn(List.of());

        scheduler.checkBestTimeNotifications();

        verify(preferenceRepository).findByQueueId(queueId);
        verifyNoInteractions(userRepository, fcmService);
    }

    @Test
    void shouldNotNotifyWhenPreferencesExistButNotifyOnBestTimeFalse() {
        Queue queue = createQueueWithWaitingCount(2);
        NotificationPreference pref = createPreference(userId1, false, true);
        when(queueRepository.findByIsActive(true)).thenReturn(List.of(queue));
        when(preferenceRepository.findByQueueId(queueId)).thenReturn(List.of(pref));

        scheduler.checkBestTimeNotifications();

        verify(preferenceRepository).findByQueueId(queueId);
        verifyNoInteractions(userRepository, fcmService);
    }

    @Test
    void shouldNotNotifyWhenUserHasNoFcmTokens() {
        Queue queue = createQueueWithWaitingCount(2);
        NotificationPreference pref = createPreference(userId1, true, true);
        User user = createUser(userId1, List.of());

        when(queueRepository.findByIsActive(true)).thenReturn(List.of(queue));
        when(preferenceRepository.findByQueueId(queueId)).thenReturn(List.of(pref));
        when(userRepository.findAllById(anySet())).thenReturn(List.of(user));

        scheduler.checkBestTimeNotifications();

        verify(fcmService, never()).sendMulticast(anyList(), anyString(), anyString(), anyString());
        verify(preferenceRepository, never()).save(any()); // no update because no notification sent
    }

    @Test
    void shouldSendNotificationAndUpdateLastSentTime() {
        Queue queue = createQueueWithWaitingCount(2);
        NotificationPreference pref = createPreference(userId1, true, true);
        User user = createUser(userId1, List.of("fcm1", "fcm2"));

        when(queueRepository.findByIsActive(true)).thenReturn(List.of(queue));
        when(preferenceRepository.findByQueueId(queueId)).thenReturn(List.of(pref));
        when(userRepository.findAllById(anySet())).thenReturn(List.of(user));

        scheduler.checkBestTimeNotifications();

        verify(fcmService).sendMulticast(
                eq(List.of("fcm1", "fcm2")),
                anyString(),
                contains("2 people"),
                eq(queueId)
        );
        verify(preferenceRepository).save(argThat(p ->
                p.getLastBestTimeNotificationSent() != null &&
                        p.getLastBestTimeNotificationSent().isAfter(LocalDateTime.now().minusMinutes(1))
        ));
    }

    @Test
    void shouldNotSendAgainWithin24Hours() {
        Queue queue = createQueueWithWaitingCount(2);
        NotificationPreference pref = createPreference(userId1, true, true);
        // Set last sent to 23 hours ago (should still be considered recent)
        pref.setLastBestTimeNotificationSent(LocalDateTime.now().minusHours(23));
        User user = createUser(userId1, List.of("fcm1"));

        when(queueRepository.findByIsActive(true)).thenReturn(List.of(queue));
        when(preferenceRepository.findByQueueId(queueId)).thenReturn(List.of(pref));
        when(userRepository.findAllById(anySet())).thenReturn(List.of(user));

        scheduler.checkBestTimeNotifications();

        verify(fcmService, never()).sendMulticast(anyList(), anyString(), anyString(), anyString());
        verify(preferenceRepository, never()).save(any());
    }

    @Test
    void shouldSendAgainAfter24Hours() {
        Queue queue = createQueueWithWaitingCount(2);
        NotificationPreference pref = createPreference(userId1, true, true);
        // Last sent 25 hours ago – should send again
        pref.setLastBestTimeNotificationSent(LocalDateTime.now().minusHours(25));
        User user = createUser(userId1, List.of("fcm1"));

        when(queueRepository.findByIsActive(true)).thenReturn(List.of(queue));
        when(preferenceRepository.findByQueueId(queueId)).thenReturn(List.of(pref));
        when(userRepository.findAllById(anySet())).thenReturn(List.of(user));

        scheduler.checkBestTimeNotifications();

        verify(fcmService).sendMulticast(anyList(), anyString(), anyString(), eq(queueId));
        verify(preferenceRepository).save(any());
    }

    @Test
    void shouldHandleMultipleUsersAndQueues() {
        Queue queue1 = createQueueWithWaitingCount(2);
        queue1.setId("queue1");
        Queue queue2 = createQueueWithWaitingCount(1);
        queue2.setId("queue2");

        // Preferences for queue1
        NotificationPreference pref1 = createPreference(userId1, true, true);
        pref1.setQueueId("queue1");
        NotificationPreference pref2 = createPreference(userId2, true, true);
        pref2.setQueueId("queue1");

        // Preferences for queue2
        NotificationPreference pref3 = createPreference(userId1, true, true);
        pref3.setQueueId("queue2");

        when(queueRepository.findByIsActive(true)).thenReturn(List.of(queue1, queue2));
        when(preferenceRepository.findByQueueId("queue1")).thenReturn(List.of(pref1, pref2));
        when(preferenceRepository.findByQueueId("queue2")).thenReturn(List.of(pref3));

        User user1 = createUser(userId1, List.of("fcm1"));
        User user2 = createUser(userId2, List.of("fcm2"));

        // Correct mock: return all users whose IDs are in the request
        when(userRepository.findAllById(anySet())).thenAnswer(inv -> {
            Set<String> ids = (Set<String>) inv.getArgument(0);
            List<User> result = new ArrayList<>();
            if (ids.contains(userId1)) result.add(user1);
            if (ids.contains(userId2)) result.add(user2);
            return result;
        });

        scheduler.checkBestTimeNotifications();

        verify(fcmService, times(3)).sendMulticast(anyList(), anyString(), anyString(), anyString());
        verify(preferenceRepository, times(3)).save(any());
    }
}