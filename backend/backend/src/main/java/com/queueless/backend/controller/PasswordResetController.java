package com.queueless.backend.controller;

import com.queueless.backend.dto.ForgotPasswordRequest;
import com.queueless.backend.dto.ResetPasswordRequest;
import com.queueless.backend.dto.VerifyOtpRequest;
import com.queueless.backend.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/password")
@RequiredArgsConstructor
@Tag(name = "Password Reset", description = "Endpoints for password reset via OTP")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @PostMapping("/forgot")
    @Operation(summary = "Request password reset", description = "Sends an OTP to the user's email for password reset.")
    @ApiResponse(responseCode = "200", description = "OTP sent successfully",
            content = @Content(schema = @Schema(example = "OTP sent to email")))
    @ApiResponse(responseCode = "404", description = "User not found")
    @ApiResponse(responseCode = "500", description = "Email sending failed")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) throws MessagingException {
        log.info("Forgot password request for email: {}", request.getEmail());
        String response = passwordResetService.sendOtp(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify OTP", description = "Verifies the OTP sent to the user's email.")
    @ApiResponse(responseCode = "200", description = "OTP verified",
            content = @Content(schema = @Schema(example = "OTP verified")))
    @ApiResponse(responseCode = "400", description = "Invalid or expired OTP")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        log.info("Verify OTP request for email: {}", request.getEmail());
        String response = passwordResetService.verifyOtp(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset")
    @Operation(summary = "Reset password", description = "Resets the user's password after OTP verification.")
    @ApiResponse(responseCode = "200", description = "Password reset successful",
            content = @Content(schema = @Schema(example = "Password reset successful")))
    @ApiResponse(responseCode = "400", description = "Passwords do not match, OTP not verified, or user not found")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("Reset password request for email: {}", request.getEmail());
        String response = passwordResetService.resetPassword(request);
        return ResponseEntity.ok(response);
    }
}