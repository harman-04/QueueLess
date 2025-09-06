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
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Component
@RequiredArgsConstructor
public class StompJwtChannelInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(StompJwtChannelInterceptor.class);
    private final JwtTokenProvider jwtProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            try {
                List<String> authHeaders = accessor.getNativeHeader("Authorization");
                if (authHeaders == null || authHeaders.isEmpty()) {
                    throw new AuthenticationCredentialsNotFoundException("Missing authorization header");
                }

                String header = authHeaders.get(0);
                String token = header.startsWith("Bearer ") ? header.substring(7) : header;

                if (!jwtProvider.validateToken(token)) {
                    throw new BadCredentialsException("Invalid token");
                }

                String userId = jwtProvider.getUserIdFromToken(token);
                String email = jwtProvider.getEmailFromToken(token);
                Collection<GrantedAuthority> authorities = jwtProvider.getAuthoritiesFromToken(token);

                // Use userId as principal
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, null, authorities);

                // Store additional details
                authentication.setDetails(new HashMap<String, Object>() {{
                    put("userId", userId);
                    put("email", email);
                }});

                SecurityContextHolder.getContext().setAuthentication(authentication);
                accessor.setUser(authentication);

                log.info("STOMP CONNECT authenticated for: {} (userId: {})", email, userId);
            } catch (Exception e) {
                log.error("WebSocket authentication failed", e);
                throw new AuthenticationCredentialsNotFoundException("WebSocket authentication failed", e);
            }
        }

        return message;
    }
}