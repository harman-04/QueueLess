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

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtProvider;
    private final TokenRepository tokenRepository;

    public JwtResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login failed: User not found for email {}", request.getEmail());
                    return new RuntimeException("User not found");
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Login failed: Invalid credentials for email {}", request.getEmail());
            throw new RuntimeException("Invalid credentials");
        }

        if (user.getRole() == Role.ADMIN) {
            log.info("Admin login detected for email: {}", user.getEmail());

            Token token = tokenRepository.findByCreatedForEmail(user.getEmail())
                    .orElseThrow(() -> {
                        log.warn("Admin login failed: No valid token found for email {}", user.getEmail());
                        return new RuntimeException("No valid admin token found");
                    });

            if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
                log.warn("Admin login failed: Token expired for email {}", user.getEmail());
                throw new RuntimeException("Admin registration token expired. Cannot login.");
            }
        }

        String jwt = jwtProvider.generateToken(user);
        log.info("Login successful for email: {}", user.getEmail());
        return new JwtResponse(jwt, user.getRole().name(), user.getId(), user.getName());
    }

    public String register(RegisterRequest request) {
        log.info("Registration attempt for email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed: Email already registered - {}", request.getEmail());
            throw new RuntimeException("Email already registered");
        }

        if (request.getRole() == Role.ADMIN || request.getRole() == Role.PROVIDER) {
            log.info("Token validation required for role: {}", request.getRole());

            if (request.getToken() == null || request.getToken().isEmpty()) {
                log.warn("Registration failed: Missing token for role {}", request.getRole());
                throw new RuntimeException("Token is required for role " + request.getRole());
            }

            Token token = tokenRepository.findByTokenValue(request.getToken())
                    .orElseThrow(() -> {
                        log.warn("Registration failed: Invalid token value {}", request.getToken());
                        return new RuntimeException("Invalid token");
                    });

            if (token.isUsed()) {
                log.warn("Registration failed: Token already used - {}", request.getToken());
                throw new RuntimeException("Token already used");
            }

            if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
                log.warn("Registration failed: Token expired - {}", request.getToken());
                throw new RuntimeException("Token expired");
            }

            if (!token.getRole().equals(request.getRole())) {
                log.warn("Registration failed: Token role mismatch. Expected {}, found {}", request.getRole(), token.getRole());
                throw new RuntimeException("Token not valid for this role");
            }

            if (!token.getCreatedForEmail().equals(request.getEmail())) {
                log.warn("Registration failed: Token email mismatch. Expected {}, found {}", request.getEmail(), token.getCreatedForEmail());
                throw new RuntimeException("Token is tied to a different email");
            }

            token.setUsed(true);
            tokenRepository.save(token);
            log.info("Token marked as used for email: {}", request.getEmail());
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .role(request.getRole())
                .build();

        userRepository.save(user);
        log.info("User registered successfully: {}", user.getEmail());
        return "User registered successfully!";
    }
}