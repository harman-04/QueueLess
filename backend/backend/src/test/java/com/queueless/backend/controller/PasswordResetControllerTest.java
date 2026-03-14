package com.queueless.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.queueless.backend.dto.ForgotPasswordRequest;
import com.queueless.backend.dto.ResetPasswordRequest;
import com.queueless.backend.dto.VerifyOtpRequest;
import com.queueless.backend.service.PasswordResetService;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.cache.type=none",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration"
})
@AutoConfigureMockMvc
class PasswordResetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PasswordResetService passwordResetService;

    @Autowired
    private ObjectMapper objectMapper;

    private final String email = "test@example.com";
    private final String otp = "123456";

    // ==================== FORGOT PASSWORD ====================

    @Test
    void forgotPassword_Success() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest(email);
        when(passwordResetService.sendOtp(any(ForgotPasswordRequest.class))).thenReturn("OTP sent to email");

        mockMvc.perform(post("/api/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("OTP sent to email"));
    }

    @Test
    void forgotPassword_UserNotFound() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest(email);
        doThrow(new RuntimeException("User not found")).when(passwordResetService).sendOtp(any(ForgotPasswordRequest.class));

        mockMvc.perform(post("/api/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    @Test
    void forgotPassword_EmailSendingFailed() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest(email);
        doThrow(new MessagingException("SMTP error")).when(passwordResetService).sendOtp(any(ForgotPasswordRequest.class));

        mockMvc.perform(post("/api/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }

    // ==================== VERIFY OTP ====================

    @Test
    void verifyOtp_Success() throws Exception {
        VerifyOtpRequest request = new VerifyOtpRequest(email, otp);
        when(passwordResetService.verifyOtp(any(VerifyOtpRequest.class))).thenReturn("OTP verified");

        mockMvc.perform(post("/api/password/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("OTP verified"));
    }

    @Test
    void verifyOtp_InvalidOtp() throws Exception {
        VerifyOtpRequest request = new VerifyOtpRequest(email, "wrong");
        doThrow(new RuntimeException("Invalid OTP")).when(passwordResetService).verifyOtp(any(VerifyOtpRequest.class));

        mockMvc.perform(post("/api/password/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid OTP"));
    }

    @Test
    void verifyOtp_ExpiredOtp() throws Exception {
        VerifyOtpRequest request = new VerifyOtpRequest(email, otp);
        doThrow(new RuntimeException("OTP expired")).when(passwordResetService).verifyOtp(any(VerifyOtpRequest.class));

        mockMvc.perform(post("/api/password/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("OTP expired"));
    }

    // ==================== RESET PASSWORD ====================

    @Test
    void resetPassword_Success() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest(email, "newPassword123", "newPassword123");
        when(passwordResetService.resetPassword(any(ResetPasswordRequest.class))).thenReturn("Password reset successful");

        mockMvc.perform(post("/api/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Password reset successful"));
    }

    @Test
    void resetPassword_PasswordsDoNotMatch() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest(email, "newPassword123", "different");
        doThrow(new RuntimeException("Passwords do not match")).when(passwordResetService).resetPassword(any(ResetPasswordRequest.class));

        mockMvc.perform(post("/api/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Passwords do not match"));
    }

    @Test
    void resetPassword_OtpNotVerified() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest(email, "newPassword123", "newPassword123");
        doThrow(new RuntimeException("OTP not verified for this email")).when(passwordResetService).resetPassword(any(ResetPasswordRequest.class));

        mockMvc.perform(post("/api/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("OTP not verified for this email"));
    }

    @Test
    void resetPassword_UserNotFound() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest(email, "newPassword123", "newPassword123");
        doThrow(new RuntimeException("User not found")).when(passwordResetService).resetPassword(any(ResetPasswordRequest.class));

        mockMvc.perform(post("/api/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("User not found"));
    }
}