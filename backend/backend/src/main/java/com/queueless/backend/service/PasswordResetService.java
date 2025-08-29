package com.queueless.backend.service;

import com.queueless.backend.dto.*;
import com.queueless.backend.model.*;
import com.queueless.backend.repository.*;
import com.queueless.backend.security.OtpVerificationStore;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final OtpRepository otpRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final OtpVerificationStore otpVerificationStore;

    public String sendOtp(ForgotPasswordRequest request) throws MessagingException {
        Optional<User> user = userRepository.findByEmail(request.getEmail());
        if (user.isEmpty()) throw new RuntimeException("User not found");

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

        // Send OTP via email
        emailService.sendOtpEmail(request.getEmail(), otp);

        return "OTP sent to email";
    }

//    public String verifyOtp(VerifyOtpRequest request) {
//        Optional<OtpDocument> otpDocOpt = otpRepository.findByEmail(request.getEmail());
//        if (otpDocOpt.isEmpty()) throw new RuntimeException("OTP not found");
//
//        OtpDocument otpDoc = otpDocOpt.get();
//        if (!otpDoc.getOtp().equals(request.getOtp())) {
//            throw new RuntimeException("Invalid OTP");
//        }
//
//        if (otpDoc.getExpiryTime().isBefore(LocalDateTime.now())) {
//            throw new RuntimeException("OTP expired");
//        }
//
//        return "OTP verified";
//    }

    public String verifyOtp(VerifyOtpRequest request) {
        Optional<OtpDocument> otpDocOpt = otpRepository.findByEmail(request.getEmail());
        if (otpDocOpt.isEmpty()) throw new RuntimeException("OTP not found");

        OtpDocument otpDoc = otpDocOpt.get();
        if (!otpDoc.getOtp().equals(request.getOtp())) {
            throw new RuntimeException("Invalid OTP");
        }

        if (otpDoc.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP expired");
        }

        otpVerificationStore.markVerified(request.getEmail()); // ✅ Mark email as verified
        return "OTP verified";
    }

//
//    public String resetPassword(ResetPasswordRequest request) {
//        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
//            throw new RuntimeException("Passwords do not match");
//        }
//
//        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
//        if (userOpt.isEmpty()) throw new RuntimeException("User not found");
//
//        User user = userOpt.get();
//        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
//        userRepository.save(user);
//
//        // Delete used OTP
//        otpRepository.deleteByEmail(request.getEmail());
//
//        return "Password reset successful";
//    }

    public String resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Passwords do not match");
        }

        if (!otpVerificationStore.isVerified(request.getEmail())) {
            throw new RuntimeException("OTP not verified for this email");
        }

        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
        if (userOpt.isEmpty()) throw new RuntimeException("User not found");

        User user = userOpt.get();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        otpVerificationStore.remove(request.getEmail());       // ✅ Clear verification after use
        otpRepository.deleteByEmail(request.getEmail());       // ✅ Optional cleanup

        return "Password reset successful";
    }

}


//public ResponseEntity<String> resetPassword(String email, ResetPasswordRequest request) {
//    if (!request.getNewPassword().equals(request.getConfirmPassword())) {
//        return ResponseEntity.badRequest().body("Passwords do not match");
//    }
//
//    Optional<User> optionalUser = userRepository.findByEmail(email);
//    if (optionalUser.isEmpty()) {
//        return ResponseEntity.badRequest().body("User not found");
//    }
//
//    User user = optionalUser.get();
//    user.setPassword(passwordEncoder.encode(request.getNewPassword()));
//    userRepository.save(user);
//
//    return ResponseEntity.ok("Password reset successfully");
//}