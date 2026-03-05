package com.queueless.backend.controller;

import com.queueless.backend.dto.JwtResponse;
import com.queueless.backend.dto.LoginRequest;
import com.queueless.backend.dto.RegisterRequest;
import com.queueless.backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Endpoints for user registration and login")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a new user account. For ADMIN or PROVIDER roles, a valid token is required.")
    @ApiResponse(responseCode = "200", description = "User registered successfully",
            content = @Content(schema = @Schema(example = "User registered successfully!")))
    @ApiResponse(responseCode = "400", description = "Validation error or business logic error (e.g., email already exists)")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        log.debug("API call: /api/auth/register for email: {}", request.getEmail());
        try {
            String response = authService.register(request);
            log.info("Registration API response for {}: {}", request.getEmail(), response);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Registration failed for {}: {}", request.getEmail(), e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    @Operation(summary = "Login user", description = "Authenticates a user and returns a JWT token")
    @ApiResponse(responseCode = "200", description = "Successful login",
            content = @Content(schema = @Schema(implementation = JwtResponse.class)))
    @ApiResponse(responseCode = "401", description = "Invalid credentials")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        log.debug("API call: /api/auth/login for email: {}", request.getEmail());
        try {
            JwtResponse response = authService.login(request);
            log.info("Login API successful for {} with role {}", response.getUserId(), response.getRole());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Login failed for email: {}. Error: {}", request.getEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }


    @Data
    public static class VerifyEmailRequest {
        @NotBlank @Email
        private String email;
        @NotBlank
        private String otp;
        // getters/setters
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify email with OTP")
    @ApiResponse(responseCode = "200", description = "Email verified")
    @ApiResponse(responseCode = "400", description = "Invalid or expired OTP")
    public ResponseEntity<?> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        try {
            String response = authService.verifyEmail(request.getEmail(), request.getOtp());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Resend verification OTP")
    public ResponseEntity<?> resendVerification(@RequestParam String email) {
        try {
            String response = authService.resendVerificationOtp(email);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}