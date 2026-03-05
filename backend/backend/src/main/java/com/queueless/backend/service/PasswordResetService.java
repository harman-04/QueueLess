package com.queueless.backend.service;

import com.queueless.backend.dto.ForgotPasswordRequest;
import com.queueless.backend.dto.ResetPasswordRequest;
import com.queueless.backend.dto.VerifyOtpRequest;
import com.queueless.backend.model.OtpDocument;
import com.queueless.backend.model.User;
import com.queueless.backend.repository.OtpRepository;
import com.queueless.backend.repository.UserRepository;
import com.queueless.backend.security.OtpVerificationStore;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final OtpRepository otpRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final OtpVerificationStore otpVerificationStore;

    public String sendOtp(ForgotPasswordRequest request) throws MessagingException {
        log.info("sendOtp for email: {}", request.getEmail());
        Optional<User> user = userRepository.findByEmail(request.getEmail());
        if (user.isEmpty()) {
            log.warn("User not found for email: {}", request.getEmail());
            throw new RuntimeException("User not found");
        }

        // Clean previous OTPs
        otpRepository.deleteByEmail(request.getEmail());

        // Generate OTP
        String otp = String.valueOf(new Random().nextInt(900000) + 100000); // 6-digit

        // Save OTP
        OtpDocument otpDoc = OtpDocument.builder()
                .email(request.getEmail())
                .otp(otp)
                .expiryTime(LocalDateTime.now().plusMinutes(5))
                .build();
        otpRepository.save(otpDoc);
        log.debug("OTP saved for email: {}", request.getEmail());

        // Send OTP via email
        emailService.sendOtpEmail(request.getEmail(), otp);
        log.info("OTP sent to email: {}", request.getEmail());

        return "OTP sent to email";
    }

    public String verifyOtp(VerifyOtpRequest request) {
        log.info("verifyOtp for email: {}", request.getEmail());
        Optional<OtpDocument> otpDocOpt = otpRepository.findByEmail(request.getEmail());
        if (otpDocOpt.isEmpty()) {
            log.warn("OTP not found for email: {}", request.getEmail());
            throw new RuntimeException("OTP not found");
        }

        OtpDocument otpDoc = otpDocOpt.get();
        if (!otpDoc.getOtp().equals(request.getOtp())) {
            log.warn("Invalid OTP for email: {}", request.getEmail());
            throw new RuntimeException("Invalid OTP");
        }

        if (otpDoc.getExpiryTime().isBefore(LocalDateTime.now())) {
            log.warn("OTP expired for email: {}", request.getEmail());
            throw new RuntimeException("OTP expired");
        }

        otpVerificationStore.markVerified(request.getEmail());
        log.info("OTP verified for email: {}", request.getEmail());
        return "OTP verified";
    }

    public String resetPassword(ResetPasswordRequest request) {
        log.info("resetPassword for email: {}", request.getEmail());
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            log.warn("Passwords do not match for email: {}", request.getEmail());
            throw new RuntimeException("Passwords do not match");
        }

        if (!otpVerificationStore.isVerified(request.getEmail())) {
            log.warn("OTP not verified for email: {}", request.getEmail());
            throw new RuntimeException("OTP not verified for this email");
        }

        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
        if (userOpt.isEmpty()) {
            log.warn("User not found for email: {}", request.getEmail());
            throw new RuntimeException("User not found");
        }

        User user = userOpt.get();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password reset for user: {}", user.getId());

        otpVerificationStore.remove(request.getEmail());
        otpRepository.deleteByEmail(request.getEmail());
        log.debug("OTP verification removed for email: {}", request.getEmail());

        return "Password reset successful";
    }
}