// Enhanced AuthService with better validation
package com.queueless.backend.service;

import com.queueless.backend.dto.JwtResponse;
import com.queueless.backend.dto.LoginRequest;
import com.queueless.backend.dto.RegisterRequest;
import com.queueless.backend.model.Role;
import com.queueless.backend.model.Token;
import com.queueless.backend.model.User;
import com.queueless.backend.repository.TokenRepository;
import com.queueless.backend.repository.UserRepository;
import com.queueless.backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtProvider;
    private final TokenRepository tokenRepository;

    public JwtResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login failed - User not found: {}", request.getEmail());
                    throw new RuntimeException("Invalid credentials");
                });

        log.debug("Stored password hash: {}", user.getPassword());
        log.debug("Input password: {}", request.getPassword());

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Login failed - Password mismatch for email: {}", request.getEmail());
            throw new RuntimeException("Invalid credentials");
        }
        // Check if user is verified
        if (user.getRole() != Role.USER && !user.getIsVerified()) {
            log.warn("Login failed - User not verified: {}", request.getEmail());
            throw new RuntimeException("Account not verified. Please contact administrator.");
        }

        String jwtToken = jwtProvider.generateToken(user);
        log.info("Login successful for email: {} | Role: {}", user.getEmail(), user.getRole());

        return new JwtResponse(
                jwtToken,
                user.getRole().name(),
                user.getId(),
                user.getName(),
                user.getProfileImageUrl(),
                user.getPlaceId(),
                user.getIsVerified(),
                user.getPreferences(),
                user.getOwnedPlaceIds()
        );
    }

    public String register(RegisterRequest request) {
        log.info("Registration attempt for email: {} | Role: {}", request.getEmail(), request.getRole());

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed - Email already registered: {}", request.getEmail());
            throw new RuntimeException("Email already registered");
        }

        // Variable to track if the account should be verified
        boolean isVerifiedAutomatically = false;

        // 🔹 Validate token for ADMIN or PROVIDER
        if (request.getRole() == Role.ADMIN || request.getRole() == Role.PROVIDER) {
            if (request.getToken() == null || request.getToken().isEmpty()) {
                log.error("Registration failed - Token required for role: {}", request.getRole());
                throw new RuntimeException("Token is required for role " + request.getRole());
            }

            Token token = tokenRepository.findByTokenValue(request.getToken())
                    .orElseThrow(() -> {
                        log.error("Registration failed - Invalid token for email: {}", request.getEmail());
                        return new RuntimeException("Invalid token");
                    });

            if (token.isUsed()) {
                log.error("Registration failed - Token already used: {}", request.getToken());
                throw new RuntimeException("Token already used");
            }

            if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
                log.error("Registration failed - Token expired: {}", request.getToken());
                throw new RuntimeException("Token expired");
            }

            if (!token.getRole().equals(request.getRole())) {
                log.error("Registration failed - Token role mismatch. Token role: {}, Request role: {}",
                        token.getRole(), request.getRole());
                throw new RuntimeException("Token not valid for this role");
            }

            if (!token.getCreatedForEmail().equals(request.getEmail())) {
                log.error("Registration failed - Token tied to different email. Expected: {}, Provided: {}",
                        token.getCreatedForEmail(), request.getEmail());
                throw new RuntimeException("Token is tied to a different email");
            }

            // ✅ Mark token used and set verification flag
            token.setUsed(true);
            tokenRepository.save(token);
            log.debug("Token {} marked as used for {}", token.getTokenValue(), request.getEmail());
            isVerifiedAutomatically = true; // Set flag to true
        }

        // Build user preferences
        User.UserPreferences userPreferences = null;
        if (request.getPreferences() != null) {
            userPreferences = User.UserPreferences.builder()
                    .emailNotifications(request.getPreferences().getEmailNotifications())
                    .smsNotifications(request.getPreferences().getSmsNotifications())
                    .language(request.getPreferences().getLanguage())
                    .defaultSearchRadius(request.getPreferences().getDefaultSearchRadius())
                    .darkMode(request.getPreferences().getDarkMode())
                    .favoritePlaceIds(new ArrayList<>())
                    .build();
        } else {
            // Default preferences
            userPreferences = User.UserPreferences.builder()
                    .emailNotifications(true)
                    .smsNotifications(false)
                    .language("en")
                    .defaultSearchRadius(5)
                    .darkMode(false)
                    .favoritePlaceIds(new ArrayList<>())
                    .build();
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .role(request.getRole())
                .placeId(request.getPlaceId())
                .profileImageUrl(null)
                .isVerified(isVerifiedAutomatically) // Now uses the dynamic flag
                .preferences(userPreferences)
                .ownedPlaceIds(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.save(user);
        log.info("Registration successful for email: {} | Role: {}", user.getEmail(), user.getRole());

        return "User registered successfully!";
    }
}