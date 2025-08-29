// src/main/java/com/queueless/backend/controller/QueueWebSocketController.java

package com.queueless.backend.controller;


import com.queueless.backend.dto.ConnectRequest;

import com.queueless.backend.dto.ServeNextRequest;

import com.queueless.backend.enums.TokenStatus;
import com.queueless.backend.model.Queue;

import com.queueless.backend.model.QueueToken;

import com.queueless.backend.service.QueueService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.messaging.handler.annotation.Header;

import org.springframework.messaging.handler.annotation.MessageMapping;

import org.springframework.messaging.handler.annotation.Payload;

import org.springframework.messaging.simp.SimpMessagingTemplate;

import org.springframework.stereotype.Controller;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;


@Controller

@Slf4j

public class QueueWebSocketController {


    private final SimpMessagingTemplate messagingTemplate;

    private final QueueService queueService;


    @Autowired

    public QueueWebSocketController(SimpMessagingTemplate messagingTemplate, QueueService queueService) {

        this.messagingTemplate = messagingTemplate;

        this.queueService = queueService;

    }


    @MessageMapping("/queue/connect")

    public void onConnect(@Payload ConnectRequest request, @Header("simpSessionId") String sessionId) {

        log.info("🎯 Client connected and requesting queue for ID: {}", request.getQueueId());


        try {

            Queue queue = queueService.getQueueById(request.getQueueId());


            if (queue != null) {

                messagingTemplate.convertAndSendToUser(sessionId, "/topic/queues/" + request.getQueueId(), queue);

                log.info("✅ Sent initial queue state for ID: {}", request.getQueueId());

            } else {

                log.warn("⚠️ Queue not found for ID: {}", request.getQueueId());

            }

        } catch (Exception e) {

            log.error("Error fetching queue on connect: {}", e.getMessage());

        }

    }


    // In QueueWebSocketController.java - Update the serveNext method
    @MessageMapping("/queue/serve-next")
    public void serveNext(@Payload ServeNextRequest request) {
        log.info("🎯 Request to serve-next for queue: {}", request.getQueueId());

        try {
            Queue updatedQueue = queueService.serveNextToken(request.getQueueId());

            if (updatedQueue != null) {
                // Find the token that is now 'in-service' for logging purposes
                Optional<QueueToken> inServiceToken = updatedQueue.getTokens().stream()
                        .filter(token -> TokenStatus.IN_SERVICE.toString().equals(token.getStatus()))
                        .findFirst();

                if (inServiceToken.isPresent()) {
                    log.info("✅ Served token {} for queue {}", inServiceToken.get().getTokenId(), updatedQueue.getId());
                } else {
                    log.info("⚠️ No tokens available for queue {}", updatedQueue.getId());
                }
            } else {
                log.warn("⚠️ Queue not found or no tokens to serve for ID: {}", request.getQueueId());
            }
        } catch (Exception e) {
            log.error("Error serving next token: {}", e.getMessage());
        }
    }
}