package com.queueless.backend.service;

import com.queueless.backend.exception.InvalidTokenException;
import com.queueless.backend.model.PasswordResetToken;
import com.queueless.backend.model.User;
import com.queueless.backend.repository.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetTokenService {

    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;

    @Value("${app.frontend-url:https://localhost:5173}")
    private String frontendUrl;

    public String createAndSendToken(User user) {
        // Delete any existing tokens for this user
        tokenRepository.deleteByUserId(user.getId());

        // Generate a unique token
        String tokenValue = "RESET-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        PasswordResetToken token = new PasswordResetToken();
        token.setToken(tokenValue);
        token.setUserId(user.getId());
        token.setEmail(user.getEmail());
        token.setExpiryDate(LocalDateTime.now().plusHours(24));
        token.setUsed(false);

        tokenRepository.save(token);

        // Send email with link
        String resetLink = frontendUrl + "/reset-password-token?token=" + tokenValue;
        emailService.sendPasswordResetLink(user.getEmail(), resetLink);

        log.info("Password reset token created and email sent to {}", user.getEmail());
        return tokenValue;
    }

    public User validateToken(String tokenValue) {
        PasswordResetToken token = tokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired reset token"));

        if (token.isUsed()) {
            throw new InvalidTokenException("Token already used");
        }
        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Token expired");
        }

        // Token is valid – we'll return the user ID later; here we just confirm validity
        return null; // we'll modify to return userId after we have UserService
    }

    public void markTokenAsUsed(String tokenValue) {
        PasswordResetToken token = tokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new InvalidTokenException("Token not found"));
        token.setUsed(true);
        tokenRepository.save(token);
    }
}