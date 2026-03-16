package com.queueless.backend.integration;

import com.queueless.backend.dto.*;
import com.queueless.backend.enums.Role;
import com.queueless.backend.model.Token;
import com.queueless.backend.model.User;
import com.queueless.backend.repository.TokenRepository;
import com.queueless.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;

public class ProviderManagementIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private com.google.firebase.auth.FirebaseAuth firebaseAuth;

    @MockitoBean
    private com.razorpay.RazorpayClient razorpayClient;

    @MockitoBean
    private com.queueless.backend.service.EmailService emailService;

    private String adminToken;
    private String adminId;
    private String providerId;
    private String placeId;
    private String placeId2;

    @BeforeEach
    void setUp() {
        tokenRepository.deleteAll();
        userRepository.deleteAll();
        createAdminAndPlace();
    }

    private void createAdminAndPlace() {
        // Create admin token
        Token adminPurchaseToken = Token.builder()
                .tokenValue("ADMIN-" + UUID.randomUUID())
                .role(Role.ADMIN)
                .createdForEmail("admin@test.com")
                .expiryDate(LocalDateTime.now().plusYears(1))
                .isUsed(false)
                .build();
        tokenRepository.save(adminPurchaseToken);

        // Register admin
        RegisterRequest adminReq = new RegisterRequest();
        adminReq.setName("Admin");
        adminReq.setEmail("admin@test.com");
        adminReq.setPassword("AdminPass123");
        adminReq.setPhoneNumber("1111111111");
        adminReq.setRole(Role.ADMIN);
        adminReq.setToken(adminPurchaseToken.getTokenValue());
        restTemplate.postForEntity("/api/auth/register", adminReq, String.class);

        // Manually verify admin
        User admin = userRepository.findByEmail("admin@test.com").orElseThrow();
        admin.setIsVerified(true);
        userRepository.save(admin);
        adminId = admin.getId();

        // Login admin
        LoginRequest loginReq = new LoginRequest("admin@test.com", "AdminPass123");
        ResponseEntity<JwtResponse> loginRes = restTemplate.postForEntity("/api/auth/login", loginReq, JwtResponse.class);
        adminToken = loginRes.getBody().getToken();

        // Create place 1
        PlaceDTO placeDto = new PlaceDTO();
        placeDto.setName("Place 1");
        placeDto.setType("SHOP");
        placeDto.setAddress("Addr 1");
        placeDto.setLocation(new double[]{10.0, 20.0});
        placeDto.setAdminId(adminId);
        placeDto.setIsActive(true);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        HttpEntity<PlaceDTO> placeRequest = new HttpEntity<>(placeDto, headers);
        ResponseEntity<PlaceDTO> placeRes = restTemplate.postForEntity("/api/places", placeRequest, PlaceDTO.class);
        placeId = placeRes.getBody().getId();

        // Create place 2
        placeDto.setName("Place 2");
        placeDto.setAddress("Addr 2");
        placeDto.setLocation(new double[]{30.0, 40.0});
        placeRequest = new HttpEntity<>(placeDto, headers);
        placeRes = restTemplate.postForEntity("/api/places", placeRequest, PlaceDTO.class);
        placeId2 = placeRes.getBody().getId();
    }

    private String createProvider() {
        // Create provider token
        Token providerPurchaseToken = Token.builder()
                .tokenValue("PROVIDER-" + UUID.randomUUID())
                .role(Role.PROVIDER)
                .createdForEmail("provider@test.com")
                .expiryDate(LocalDateTime.now().plusYears(1))
                .isUsed(false)
                .createdByAdminId(adminId)
                .build();
        tokenRepository.save(providerPurchaseToken);

        // Register provider
        RegisterRequest providerReq = new RegisterRequest();
        providerReq.setName("Old Name");
        providerReq.setEmail("provider@test.com");
        providerReq.setPassword("ProviderPass123");
        providerReq.setPhoneNumber("2222222222");
        providerReq.setRole(Role.PROVIDER);
        providerReq.setToken(providerPurchaseToken.getTokenValue());
        providerReq.setPlaceId(placeId); // initial place

        restTemplate.postForEntity("/api/auth/register", providerReq, String.class);

        // Manually verify
        User provider = userRepository.findByEmail("provider@test.com").orElseThrow();
        provider.setIsVerified(true);
        userRepository.save(provider);
        providerId = provider.getId();
        return providerId;
    }

    @Test
    void testProviderUpdate() {
        String providerId = createProvider();

        // Update provider
        ProviderUpdateRequest updateReq = new ProviderUpdateRequest();
        updateReq.setName("New Name");
        updateReq.setEmail("newemail@test.com");
        updateReq.setPhoneNumber("3333333333");
        updateReq.setManagedPlaceIds(List.of(placeId, placeId2));
        updateReq.setIsActive(true);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        HttpEntity<ProviderUpdateRequest> updateEntity = new HttpEntity<>(updateReq, headers);
        ResponseEntity<ProviderDetailsDTO> updateRes = restTemplate.exchange(
                "/api/admin/providers/" + providerId,
                HttpMethod.PUT,
                updateEntity,
                ProviderDetailsDTO.class
        );
        assertThat(updateRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        ProviderDetailsDTO updated = updateRes.getBody();
        assertThat(updated.getName()).isEqualTo("New Name");
        assertThat(updated.getEmail()).isEqualTo("newemail@test.com");
        assertThat(updated.getPhoneNumber()).isEqualTo("3333333333");
        assertThat(updated.getManagedPlaceIds()).containsExactlyInAnyOrder(placeId, placeId2);
    }

    @Test
    void testToggleProviderStatus() throws Exception {
        String providerId = createProvider();

        // Initially active (default true)
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);

        // Toggle to inactive
        ResponseEntity<Void> toggleRes = restTemplate.exchange(
                "/api/admin/providers/" + providerId + "/status?active=false",
                HttpMethod.PATCH,
                new HttpEntity<>(headers),
                Void.class
        );
        assertThat(toggleRes.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Verify provider cannot log in – now expects 401
        LoginRequest loginReq = new LoginRequest("provider@test.com", "ProviderPass123");
        ResponseEntity<String> loginRes = restTemplate.postForEntity("/api/auth/login", loginReq, String.class);
        assertThat(loginRes.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(loginRes.getBody()).contains("disabled");

        // Toggle back to active
        toggleRes = restTemplate.exchange(
                "/api/admin/providers/" + providerId + "/status?active=true",
                HttpMethod.PATCH,
                new HttpEntity<>(headers),
                Void.class
        );
        assertThat(toggleRes.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Now should be able to log in
        ResponseEntity<JwtResponse> loginRes2 = restTemplate.postForEntity("/api/auth/login", loginReq, JwtResponse.class);
        assertThat(loginRes2.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
    @Test
    void testResetProviderPassword() throws Exception {
        String providerId = createProvider();

        // Mock email sending
        doNothing().when(emailService).sendOtpEmail(any(), any());

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        HttpEntity<?> request = new HttpEntity<>(headers);

        ResponseEntity<String> resetRes = restTemplate.exchange(
                "/api/admin/providers/" + providerId + "/reset-password",
                HttpMethod.POST,
                request,
                String.class
        );
        assertThat(resetRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resetRes.getBody()).isEqualTo("Password reset email sent to provider");
    }
}