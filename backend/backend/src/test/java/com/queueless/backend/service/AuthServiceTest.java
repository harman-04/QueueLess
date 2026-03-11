package com.queueless.backend.service;

import com.queueless.backend.dto.JwtResponse;
import com.queueless.backend.dto.LoginRequest;
import com.queueless.backend.dto.RegisterRequest;
import com.queueless.backend.enums.Role;
import com.queueless.backend.model.Token;
import com.queueless.backend.model.User;
import com.queueless.backend.repository.OtpRepository;
import com.queueless.backend.repository.TokenRepository;
import com.queueless.backend.repository.UserRepository;
import com.queueless.backend.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtProvider;

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private OtpRepository otpRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private LoginRequest loginRequest;
    private RegisterRequest registerRequest;
    private Token validAdminToken;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id("user123")
                .name("John Doe")
                .email("john@example.com")
                .password("encodedPassword")
                .phoneNumber("1234567890")
                .role(Role.USER)
                .isVerified(true)
                .preferences(User.UserPreferences.builder()
                        .emailNotifications(true)
                        .smsNotifications(false)
                        .language("en")
                        .defaultSearchRadius(5)
                        .darkMode(false)
                        .favoritePlaceIds(new ArrayList<>())
                        .build())
                .ownedPlaceIds(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .build();

        loginRequest = new LoginRequest("john@example.com", "rawPassword");

        registerRequest = new RegisterRequest();
        registerRequest.setName("Jane Doe");
        registerRequest.setEmail("jane@example.com");
        registerRequest.setPassword("Password123");
        registerRequest.setPhoneNumber("9876543210");
        registerRequest.setRole(Role.USER);

        validAdminToken = Token.builder()
                .tokenValue("ADMIN_TOKEN")
                .role(Role.ADMIN)
                .createdForEmail("jane@example.com")
                .expiryDate(LocalDateTime.now().plusDays(30))
                .isUsed(false)
                .createdByAdminId("admin123")
                .build();
    }

    // ================= LOGIN TESTS =================

    @Test
    void loginSuccess() {
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(loginRequest.getPassword(), testUser.getPassword())).thenReturn(true);
        when(jwtProvider.generateToken(testUser)).thenReturn("jwt-token");

        JwtResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals(Role.USER.name(), response.getRole());
        assertEquals(testUser.getId(), response.getUserId());
        assertEquals(testUser.getName(), response.getName());
        assertEquals(testUser.getIsVerified(), response.getIsVerified());

        verify(userRepository).findByEmail(loginRequest.getEmail());
        verify(passwordEncoder).matches(loginRequest.getPassword(), testUser.getPassword());
        verify(jwtProvider).generateToken(testUser);
    }

    @Test
    void loginUserNotFound() {
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.login(loginRequest));
        assertEquals("Invalid credentials", exception.getMessage());

        verify(userRepository).findByEmail(loginRequest.getEmail());
        verifyNoInteractions(passwordEncoder, jwtProvider);
    }

    @Test
    void loginWrongPassword() {
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(loginRequest.getPassword(), testUser.getPassword())).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.login(loginRequest));
        assertEquals("Invalid credentials", exception.getMessage());

        verify(userRepository).findByEmail(loginRequest.getEmail());
        verify(passwordEncoder).matches(loginRequest.getPassword(), testUser.getPassword());
        verifyNoInteractions(jwtProvider);
    }

    @Test
    void loginUnverifiedAdmin() {
        testUser.setRole(Role.ADMIN);
        testUser.setIsVerified(false);
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(loginRequest.getPassword(), testUser.getPassword())).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.login(loginRequest));
        assertEquals("Account not verified. Please check your email.", exception.getMessage());    }

    // ================= REGISTER TESTS =================

    @Test
    void registerUserSuccess() {
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        String result = authService.register(registerRequest);

        assertEquals("User registered successfully!", result);
        // Verify OTP logic was triggered
        verify(otpRepository).deleteByEmail(registerRequest.getEmail());
        verify(otpRepository).save(any());
        verify(emailService).sendVerificationOtpEmail(eq(registerRequest.getEmail()), anyString());
        verify(userRepository).existsByEmail(registerRequest.getEmail());
        verify(passwordEncoder).encode(registerRequest.getPassword());
        verify(userRepository).save(argThat(user -> {
            assertEquals(registerRequest.getName(), user.getName());
            assertEquals(registerRequest.getEmail(), user.getEmail());
            assertEquals("encodedPassword", user.getPassword());
            assertEquals(registerRequest.getPhoneNumber(), user.getPhoneNumber());
            assertEquals(Role.USER, user.getRole());
            assertFalse(user.getIsVerified()); // USER is not auto-verified
            assertNotNull(user.getCreatedAt());
            assertNotNull(user.getPreferences());
            return true;
        }));
    }

    @Test
    void registerUserEmailAlreadyExists() {
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.register(registerRequest));
        assertEquals("Email already registered", exception.getMessage());
    }

    @Test
    void registerAdminSuccess() {
        registerRequest.setRole(Role.ADMIN);
        registerRequest.setToken("ADMIN_TOKEN");

        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(tokenRepository.findByTokenValue("ADMIN_TOKEN")).thenReturn(Optional.of(validAdminToken));
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        String result = authService.register(registerRequest);

        assertEquals("User registered successfully!", result);
        verify(tokenRepository).findByTokenValue("ADMIN_TOKEN");
        verify(tokenRepository).save(argThat(t -> t.isUsed()));
        verify(userRepository).save(argThat(user -> {
            assertEquals(Role.ADMIN, user.getRole());
            assertTrue(user.getIsVerified()); // Admin is auto-verified
            return true;
        }));
    }

    @Test
    void registerProviderSuccess() {
        registerRequest.setRole(Role.PROVIDER);
        registerRequest.setToken("PROVIDER_TOKEN");
        registerRequest.setPlaceId("place123");

        Token providerToken = Token.builder()
                .tokenValue("PROVIDER_TOKEN")
                .role(Role.PROVIDER)
                .createdForEmail("jane@example.com")
                .expiryDate(LocalDateTime.now().plusDays(30))
                .isUsed(false)
                .createdByAdminId("admin123")
                .build();

        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        // Mock findByTokenValue to return the same token for any number of calls
        when(tokenRepository.findByTokenValue("PROVIDER_TOKEN")).thenReturn(Optional.of(providerToken));
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        String result = authService.register(registerRequest);

        assertEquals("User registered successfully!", result);
        // Verify that findByTokenValue was called at least once (it's called twice currently)
        verify(tokenRepository, atLeastOnce()).findByTokenValue("PROVIDER_TOKEN");
        // Verify that the token was saved with used=true
        verify(tokenRepository).save(argThat(t -> t.isUsed() && t.getTokenValue().equals("PROVIDER_TOKEN")));
        verify(userRepository).save(argThat(user -> {
            assertEquals(Role.PROVIDER, user.getRole());
            assertTrue(user.getIsVerified());
            assertEquals("admin123", user.getAdminId());
            assertTrue(user.getManagedPlaceIds().contains("place123"));
            return true;
        }));
    }

    @Test
    void registerAdminMissingToken() {
        registerRequest.setRole(Role.ADMIN);
        registerRequest.setToken(null);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.register(registerRequest));
        assertEquals("Token is required for role ADMIN", exception.getMessage());
    }

    @Test
    void registerInvalidToken() {
        registerRequest.setRole(Role.ADMIN);
        registerRequest.setToken("INVALID");

        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(tokenRepository.findByTokenValue("INVALID")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.register(registerRequest));
        assertEquals("Invalid token", exception.getMessage());
    }

    @Test
    void registerTokenAlreadyUsed() {
        registerRequest.setRole(Role.ADMIN);
        registerRequest.setToken("USED_TOKEN");
        validAdminToken.setUsed(true);
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(tokenRepository.findByTokenValue("USED_TOKEN")).thenReturn(Optional.of(validAdminToken));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.register(registerRequest));
        assertEquals("Token already used", exception.getMessage());
    }

    @Test
    void registerTokenExpired() {
        registerRequest.setRole(Role.ADMIN);
        registerRequest.setToken("EXPIRED_TOKEN");
        validAdminToken.setExpiryDate(LocalDateTime.now().minusDays(1));
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(tokenRepository.findByTokenValue("EXPIRED_TOKEN")).thenReturn(Optional.of(validAdminToken));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.register(registerRequest));
        assertEquals("Token expired", exception.getMessage());
    }

    @Test
    void registerTokenRoleMismatch() {
        registerRequest.setRole(Role.ADMIN);
        registerRequest.setToken("ROLE_MISMATCH");
        validAdminToken.setRole(Role.PROVIDER); // token is for PROVIDER
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(tokenRepository.findByTokenValue("ROLE_MISMATCH")).thenReturn(Optional.of(validAdminToken));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.register(registerRequest));
        assertEquals("Token not valid for this role", exception.getMessage());
    }

    @Test
    void registerTokenEmailMismatch() {
        registerRequest.setRole(Role.ADMIN);
        registerRequest.setToken("EMAIL_MISMATCH");
        validAdminToken.setCreatedForEmail("other@example.com");
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(tokenRepository.findByTokenValue("EMAIL_MISMATCH")).thenReturn(Optional.of(validAdminToken));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.register(registerRequest));
        assertEquals("Token is tied to a different email", exception.getMessage());
    }

    // ================= VERIFY EMAIL TESTS =================

    @Test
    void verifyEmailSuccess() {
        String email = "jane@example.com";
        String otp = "123456";

        com.queueless.backend.model.OtpDocument otpDoc = com.queueless.backend.model.OtpDocument.builder()
                .email(email)
                .otp(otp)
                .expiryTime(LocalDateTime.now().plusMinutes(5))
                .build();

        when(otpRepository.findByEmail(email)).thenReturn(Optional.of(otpDoc));
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        String result = authService.verifyEmail(email, otp);

        assertEquals("Email verified successfully", result);
        assertTrue(testUser.getIsVerified());
        verify(otpRepository).delete(otpDoc);
        verify(userRepository).save(testUser);
    }

    @Test
    void verifyEmailInvalidOtp() {
        String email = "jane@example.com";
        com.queueless.backend.model.OtpDocument otpDoc = com.queueless.backend.model.OtpDocument.builder()
                .email(email)
                .otp("123456")
                .expiryTime(LocalDateTime.now().plusMinutes(5))
                .build();

        when(otpRepository.findByEmail(email)).thenReturn(Optional.of(otpDoc));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.verifyEmail(email, "wrong-otp"));

        assertEquals("Invalid OTP", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void verifyEmailExpiredOtp() {
        String email = "jane@example.com";
        com.queueless.backend.model.OtpDocument otpDoc = com.queueless.backend.model.OtpDocument.builder()
                .email(email)
                .otp("123456")
                .expiryTime(LocalDateTime.now().minusMinutes(1)) // Expired
                .build();

        when(otpRepository.findByEmail(email)).thenReturn(Optional.of(otpDoc));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.verifyEmail(email, "123456"));

        assertEquals("OTP expired", exception.getMessage());
    }
}