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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OtpRepository otpRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @Mock
    private OtpVerificationStore otpVerificationStore;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private final String email = "test@example.com";
    private final String otp = "123456";
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id("user123")
                .email(email)
                .password("encodedOldPassword")
                .build();
    }

    // ================= SEND OTP =================

    @Test
    void sendOtpSuccess() throws MessagingException {
        ForgotPasswordRequest request = new ForgotPasswordRequest(email);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        doNothing().when(otpRepository).deleteByEmail(email);
        when(otpRepository.save(any(OtpDocument.class))).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(emailService).sendOtpEmail(eq(email), anyString());

        String result = passwordResetService.sendOtp(request);

        assertEquals("OTP sent to email", result);
        verify(otpRepository).deleteByEmail(email);
        verify(otpRepository).save(argThat(otpDoc ->
                otpDoc.getEmail().equals(email) &&
                        otpDoc.getOtp() != null &&
                        otpDoc.getExpiryTime().isAfter(LocalDateTime.now())
        ));
        verify(emailService).sendOtpEmail(eq(email), anyString());
    }

    @Test
    void sendOtpUserNotFound() {
        ForgotPasswordRequest request = new ForgotPasswordRequest(email);
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> passwordResetService.sendOtp(request));
        assertEquals("User not found", exception.getMessage());
        verify(otpRepository, never()).deleteByEmail(any());
        verify(emailService, never()).sendOtpEmail(any(), any());
    }

    // ================= VERIFY OTP =================

    @Test
    void verifyOtpSuccess() {
        VerifyOtpRequest request = new VerifyOtpRequest(email, otp);
        OtpDocument otpDoc = OtpDocument.builder()
                .email(email)
                .otp(otp)
                .expiryTime(LocalDateTime.now().plusMinutes(5))
                .build();

        when(otpRepository.findByEmail(email)).thenReturn(Optional.of(otpDoc));
        doNothing().when(otpVerificationStore).markVerified(email);

        String result = passwordResetService.verifyOtp(request);

        assertEquals("OTP verified", result);
        verify(otpVerificationStore).markVerified(email);
    }

    @Test
    void verifyOtpNotFound() {
        VerifyOtpRequest request = new VerifyOtpRequest(email, otp);
        when(otpRepository.findByEmail(email)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> passwordResetService.verifyOtp(request));
        assertEquals("OTP not found", exception.getMessage());
    }

    @Test
    void verifyOtpInvalid() {
        VerifyOtpRequest request = new VerifyOtpRequest(email, "wrong");
        OtpDocument otpDoc = OtpDocument.builder()
                .email(email)
                .otp(otp)
                .expiryTime(LocalDateTime.now().plusMinutes(5))
                .build();

        when(otpRepository.findByEmail(email)).thenReturn(Optional.of(otpDoc));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> passwordResetService.verifyOtp(request));
        assertEquals("Invalid OTP", exception.getMessage());
    }

    @Test
    void verifyOtpExpired() {
        VerifyOtpRequest request = new VerifyOtpRequest(email, otp);
        OtpDocument otpDoc = OtpDocument.builder()
                .email(email)
                .otp(otp)
                .expiryTime(LocalDateTime.now().minusMinutes(1))
                .build();

        when(otpRepository.findByEmail(email)).thenReturn(Optional.of(otpDoc));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> passwordResetService.verifyOtp(request));
        assertEquals("OTP expired", exception.getMessage());
    }

    // ================= RESET PASSWORD =================

    @Test
    void resetPasswordSuccess() {
        ResetPasswordRequest request = new ResetPasswordRequest(email, "newPass", "newPass");

        when(otpVerificationStore.isVerified(email)).thenReturn(true);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("newPass")).thenReturn("encodedNewPass");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(otpVerificationStore).remove(email);
        doNothing().when(otpRepository).deleteByEmail(email);

        String result = passwordResetService.resetPassword(request);

        assertEquals("Password reset successful", result);
        verify(userRepository).save(argThat(user ->
                user.getPassword().equals("encodedNewPass")
        ));
        verify(otpVerificationStore).remove(email);
        verify(otpRepository).deleteByEmail(email);
    }

    @Test
    void resetPasswordPasswordsDoNotMatch() {
        ResetPasswordRequest request = new ResetPasswordRequest(email, "newPass", "different");

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> passwordResetService.resetPassword(request));
        assertEquals("Passwords do not match", exception.getMessage());
    }

    @Test
    void resetPasswordOtpNotVerified() {
        ResetPasswordRequest request = new ResetPasswordRequest(email, "newPass", "newPass");

        when(otpVerificationStore.isVerified(email)).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> passwordResetService.resetPassword(request));
        assertEquals("OTP not verified for this email", exception.getMessage());
    }

    @Test
    void resetPasswordUserNotFound() {
        ResetPasswordRequest request = new ResetPasswordRequest(email, "newPass", "newPass");

        when(otpVerificationStore.isVerified(email)).thenReturn(true);
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> passwordResetService.resetPassword(request));
        assertEquals("User not found", exception.getMessage());
    }
}