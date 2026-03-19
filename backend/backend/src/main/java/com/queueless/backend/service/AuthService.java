package com.queueless.backend.service;

import com.queueless.backend.dto.JwtResponse;
import com.queueless.backend.dto.LoginRequest;
import com.queueless.backend.dto.RegisterRequest;
import com.queueless.backend.enums.Role;
import com.queueless.backend.exception.InvalidTokenException;
import com.queueless.backend.model.OtpDocument;
import com.queueless.backend.model.Token;
import com.queueless.backend.model.User;
import com.queueless.backend.repository.OtpRepository;
import com.queueless.backend.repository.TokenRepository;
import com.queueless.backend.repository.UserRepository;
import com.queueless.backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtProvider;
    private final TokenRepository tokenRepository;
    private final OtpRepository otpRepository;
    private final EmailService emailService;
    private final AuditLogService auditLogService;

    public JwtResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login failed - User not found: {}", request.getEmail());
                    throw new RuntimeException("Invalid credentials");
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Login failed - Password mismatch for email: {}", request.getEmail());
            throw new RuntimeException("Invalid credentials");
        }

        if (!user.getIsVerified()) {
            log.warn("Login failed - User not verified: {}", request.getEmail());
            throw new RuntimeException("Account not verified. Please check your email.");
        }

        // Check if account is active (null means active for backward compatibility)
        if (user.getIsActive() != null && !user.getIsActive()) {
            log.warn("Login failed - Account disabled for email: {}", request.getEmail());
            throw new RuntimeException("Your account has been disabled. Please contact support.");
        }

        String jwtToken = jwtProvider.generateToken(user);
        log.info("Login successful for email: {} | Role: {}", user.getEmail(), user.getRole());

        Map<String, Object> details = new HashMap<>();
        details.put("email", request.getEmail());
        auditLogService.logEvent("USER_LOGIN", "User logged in", details);

        return new JwtResponse(
                jwtToken,
                user.getRole().name(),
                user.getId(),
                user.getName(),
                user.getPhoneNumber(),
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

        // Token validation for ADMIN and PROVIDER
        Token token = null;
        if (request.getRole() == Role.ADMIN || request.getRole() == Role.PROVIDER) {
            if (request.getToken() == null || request.getToken().isEmpty()) {
                log.error("Registration failed - Token required for role: {}", request.getRole());
                throw new InvalidTokenException("Token is required for role " + request.getRole());
            }

            token = tokenRepository.findByTokenValue(request.getToken())
                    .orElseThrow(() -> {
                        log.error("Registration failed - Invalid token for email: {}", request.getEmail());
                        return new InvalidTokenException("Invalid token");
                    });

            if (token.isUsed()) {
                log.error("Registration failed - Token already used: {}", request.getToken());
                throw new InvalidTokenException("Token already used");
            }

            if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
                log.error("Registration failed - Token expired: {}", request.getToken());
                throw new InvalidTokenException("Token expired");
            }

            if (!token.getRole().equals(request.getRole())) {
                log.error("Registration failed - Token role mismatch. Token role: {}, Request role: {}",
                        token.getRole(), request.getRole());
                throw new InvalidTokenException("Token not valid for this role");
            }

            if (!token.getCreatedForEmail().equals(request.getEmail())) {
                log.error("Registration failed - Token tied to different email. Expected: {}, Provided: {}",
                        token.getCreatedForEmail(), request.getEmail());
                throw new InvalidTokenException("Token is tied to a different email");
            }

            token.setUsed(true);
            tokenRepository.save(token);
            log.debug("Token {} marked as used for {}", token.getTokenValue(), request.getEmail());
        }

        // Build user preferences
        User.UserPreferences userPreferences;
        if (request.getPreferences() != null) {
            userPreferences = User.UserPreferences.builder()
                    .emailNotifications(request.getPreferences().getEmailNotifications())
                    .smsNotifications(request.getPreferences().getSmsNotifications())
                    .pushNotifications(request.getPreferences().getPushNotifications() != null
                            ? request.getPreferences().getPushNotifications()
                            : true)
                    .language(request.getPreferences().getLanguage())
                    .defaultSearchRadius(request.getPreferences().getDefaultSearchRadius())
                    .darkMode(request.getPreferences().getDarkMode())
                    .favoritePlaceIds(new ArrayList<>())
                    .build();
        } else {
            userPreferences = User.UserPreferences.builder()
                    .emailNotifications(true)
                    .smsNotifications(false)
                    .pushNotifications(true)
                    .language("en")
                    .defaultSearchRadius(5)
                    .darkMode(false)
                    .favoritePlaceIds(new ArrayList<>())
                    .build();
        }

        // For PROVIDER, extract adminId and managed place
        String adminId = null;
        List<String> managedPlaceIds = new ArrayList<>();
        if (request.getRole() == Role.PROVIDER && token != null) {
            adminId = token.getCreatedByAdminId();
            if (request.getPlaceId() != null && !request.getPlaceId().isEmpty()) {
                managedPlaceIds.add(request.getPlaceId());
            }
        }

        // Build user – isVerified is always false initially
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .role(request.getRole())
                .placeId(request.getPlaceId())
                .profileImageUrl(null)
                .isVerified(false)   // <-- changed: always false
                .preferences(userPreferences)
                .ownedPlaceIds(new ArrayList<>())
                .adminId(adminId)
                .managedPlaceIds(managedPlaceIds)
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.save(user);

        // Send verification OTP for ALL roles
        sendVerificationOtp(request.getEmail());

        log.info("Registration successful for email: {} | Role: {}", user.getEmail(), user.getRole());

        Map<String, Object> details = new HashMap<>();
        details.put("email", request.getEmail());
        details.put("role", request.getRole().name());
        auditLogService.logEvent("USER_REGISTERED", "User registered with role: " + request.getRole(), details);
        return "User registered successfully! Please verify your email.";
    }
    private void sendVerificationOtp(String email) {
        // Clean previous OTPs
        otpRepository.deleteByEmail(email);

        // Generate 6-digit OTP
        String otp = String.valueOf(new Random().nextInt(900000) + 100000);

        // Save OTP
        OtpDocument otpDoc = OtpDocument.builder()
                .email(email)
                .otp(otp)
                .expiryTime(LocalDateTime.now().plusMinutes(5))
                .build();
        otpRepository.save(otpDoc);

        // Send email
        emailService.sendVerificationOtpEmail(email, otp);
        log.info("Verification OTP sent to {}", email);
    }

    public String verifyEmail(String email, String otp) {
        log.info("Verifying email {} with OTP", email);
        Optional<OtpDocument> otpDocOpt = otpRepository.findByEmail(email);
        if (otpDocOpt.isEmpty()) {
            throw new RuntimeException("OTP not found or expired");
        }
        OtpDocument otpDoc = otpDocOpt.get();
        if (!otpDoc.getOtp().equals(otp)) {
            throw new RuntimeException("Invalid OTP");
        }
        if (otpDoc.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP expired");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setIsVerified(true);
        userRepository.save(user);

        otpRepository.delete(otpDoc);
        log.info("Email {} verified successfully", email);
        return "Email verified successfully";
    }

    public String resendVerificationOtp(String email) {
        log.info("Resending verification OTP to {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (user.getIsVerified()) {
            throw new RuntimeException("User already verified");
        }
        sendVerificationOtp(email);
        return "Verification OTP resent";
    }
    }