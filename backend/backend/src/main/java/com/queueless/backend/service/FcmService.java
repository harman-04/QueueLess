// src/main/java/com/queueless/backend/service/FcmService.java
package com.queueless.backend.service;

import com.google.firebase.messaging.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class FcmService {

    public void sendNotification(String token, String title, String body) {
        Message message = Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .build();
        send(message);
    }

    public void sendNotificationWithData(String token, String title, String body, String queueId) {
        Message message = Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putData("queueId", queueId)
                .putData("click_action", "FLUTTER_NOTIFICATION_CLICK") // for PWA/React, this can be custom
                .build();
        send(message);
    }

    private void send(Message message) {
        try {
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("Successfully sent message: {}", response);
        } catch (FirebaseMessagingException e) {
            log.error("Failed to send FCM message: {}", e.getMessage());
        }
    }

    public void sendMulticast(List<String> tokens, String title, String body, String queueId) {
        MulticastMessage message = MulticastMessage.builder()
                .addAllTokens(tokens)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putData("queueId", queueId)
                .build();
        try {
            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);
            log.info("Successfully sent multicast: {} success, {} failure",
                    response.getSuccessCount(), response.getFailureCount());
            // Optionally handle failed tokens (remove from user's list)
        } catch (FirebaseMessagingException e) {
            log.error("Failed to send multicast message: {}", e.getMessage());
        }
    }
}