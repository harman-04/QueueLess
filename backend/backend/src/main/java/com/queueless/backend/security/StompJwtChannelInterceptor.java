package com.queueless.backend.security;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * Intercepts STOMP messages to perform JWT authentication for WebSocket connections.
 * This is crucial for securing STOMP messages before they are processed by the message broker.
 */
@Component
@RequiredArgsConstructor
public class StompJwtChannelInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(StompJwtChannelInterceptor.class);
    private final JwtTokenProvider jwtProvider;

    private static final String PRINCIPAL_SESSION_ATTRIBUTE = "principal";

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        // Check for 'connect' command
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            try {
                // Get the Authorization header from STOMP headers
                List<String> authHeaders = accessor.getNativeHeader("Authorization");
                if (authHeaders == null || authHeaders.isEmpty()) {
                    log.warn("⚠️ Missing Authorization header in STOMP CONNECT");
                    return message;
                }

                String header = authHeaders.get(0);
                String token = header.startsWith("Bearer ") ? header.substring(7) : header;

                if (!jwtProvider.validateToken(token)) {
                    log.warn("❌ Invalid/expired JWT in STOMP CONNECT");
                    return message;
                }

                String email = jwtProvider.getEmailFromToken(token);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(email, null, List.of());

                // 🔑 CRITICAL FIX: Store the authenticated user in the session attributes.
                // This makes the principal "sticky" to the session, allowing it to be
                // retrieved for subsequent messages regardless of the processing thread.
                Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
                if (sessionAttributes != null) {
                    sessionAttributes.put(PRINCIPAL_SESSION_ATTRIBUTE, authentication);
                }

                // Set the user principal on the accessor for the current message.
                accessor.setUser(authentication);
                log.info("✅ STOMP CONNECT authenticated for: {}", email);
            } catch (Exception e) {
                log.error("Error during WebSocket authentication", e);
                return null; // Drop the message if an unexpected error occurs
            }
        }
        // For all other commands (SUBSCRIBE, SEND, etc.), we retrieve the user from session attributes
        // and re-attach it to the message header accessor. This is the crucial part
        // for making authentication work correctly across threads and messages.
        else {

            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            if (sessionAttributes != null && sessionAttributes.containsKey(PRINCIPAL_SESSION_ATTRIBUTE)) {
                Principal principal = (Principal) sessionAttributes.get(PRINCIPAL_SESSION_ATTRIBUTE);
                // Inside the `else` block in preSend
                log.debug("Found principal in session for command {}: {}", accessor.getCommand(), principal);
                accessor.setUser(principal);
            }
        }

        return message;
    }
}