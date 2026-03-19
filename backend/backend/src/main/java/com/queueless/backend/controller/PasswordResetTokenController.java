package com.queueless.backend.controller;

import com.queueless.backend.dto.ResetPasswordWithTokenRequest;
import com.queueless.backend.exception.InvalidTokenException;
import com.queueless.backend.model.PasswordResetToken;
import com.queueless.backend.model.User;
import com.queueless.backend.repository.PasswordResetTokenRepository;
import com.queueless.backend.repository.UserRepository;
import com.queueless.backend.service.PasswordResetTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/password-reset-token")
@RequiredArgsConstructor
public class PasswordResetTokenController {

    private final PasswordResetTokenService tokenService;
    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestParam String token) {
        try {
            tokenService.validateToken(token);
            return ResponseEntity.ok().body("{\"valid\":true}");
        } catch (InvalidTokenException e) {
            return ResponseEntity.badRequest().body("{\"valid\":false, \"message\":\"" + e.getMessage() + "\"}");
        }
    }

    @PostMapping("/reset")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordWithTokenRequest request) {
        log.info("Password reset attempt with token");

        // Validate token and get user
        PasswordResetToken tokenEntity = tokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new InvalidTokenException("Invalid token"));

        if (tokenEntity.isUsed()) {
            throw new InvalidTokenException("Token already used");
        }
        if (tokenEntity.getExpiryDate().isBefore(java.time.LocalDateTime.now())) {
            throw new InvalidTokenException("Token expired");
        }

        User user = userRepository.findById(tokenEntity.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Mark token as used
        tokenEntity.setUsed(true);
        tokenRepository.save(tokenEntity);

        log.info("Password reset successful for user: {}", user.getEmail());
        return ResponseEntity.ok("Password reset successful");
    }
}