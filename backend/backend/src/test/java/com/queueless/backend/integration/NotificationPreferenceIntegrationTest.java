package com.queueless.backend.integration;

import com.queueless.backend.dto.*;
import com.queueless.backend.enums.Role;
import com.queueless.backend.model.Queue;
import com.queueless.backend.model.Token;
import com.queueless.backend.model.User;
import com.queueless.backend.repository.NotificationPreferenceRepository;
import com.queueless.backend.repository.TokenRepository;
import com.queueless.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class NotificationPreferenceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationPreferenceRepository preferenceRepository;

    @MockitoBean
    private com.google.firebase.auth.FirebaseAuth firebaseAuth;

    @MockitoBean
    private com.razorpay.RazorpayClient razorpayClient;

    private String adminId;
    private String adminToken;
    private String placeId;
    private String placeId2;
    private String queueId;
    private String userId;
    private String userToken;

    @BeforeEach
    void setUp() {
        preferenceRepository.deleteAll();
        tokenRepository.deleteAll();
        userRepository.deleteAll();

        createAdminAndPlaces();
        queueId = createQueue(placeId);
        userId = createUser();
        userToken = loginUser();
    }

    private void createAdminAndPlaces() {
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
        ResponseEntity<String> regRes = restTemplate.postForEntity("/api/auth/register", adminReq, String.class);
        assertThat(regRes.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Manually verify admin
        User admin = userRepository.findByEmail("admin@test.com").orElseThrow();
        admin.setIsVerified(true);
        userRepository.save(admin);
        adminId = admin.getId();

        // Login admin to get token for place creation
        LoginRequest loginReq = new LoginRequest("admin@test.com", "AdminPass123");
        ResponseEntity<JwtResponse> loginRes = restTemplate.postForEntity("/api/auth/login", loginReq, JwtResponse.class);
        assertThat(loginRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        adminToken = loginRes.getBody().getToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Create place 1
        PlaceDTO placeDto = new PlaceDTO();
        placeDto.setName("Place 1");
        placeDto.setType("SHOP");
        placeDto.setAddress("Addr 1");
        placeDto.setLocation(new double[]{10.0, 20.0});
        placeDto.setAdminId(adminId);
        placeDto.setIsActive(true);
        HttpEntity<PlaceDTO> placeRequest = new HttpEntity<>(placeDto, headers);
        ResponseEntity<PlaceDTO> placeRes = restTemplate.postForEntity("/api/places", placeRequest, PlaceDTO.class);
        assertThat(placeRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        placeId = placeRes.getBody().getId();

        // Create place 2
        placeDto.setName("Place 2");
        placeDto.setAddress("Addr 2");
        placeDto.setLocation(new double[]{30.0, 40.0});
        placeRequest = new HttpEntity<>(placeDto, headers);
        placeRes = restTemplate.postForEntity("/api/places", placeRequest, PlaceDTO.class);
        assertThat(placeRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        placeId2 = placeRes.getBody().getId();
    }

    private String createQueue(String placeId) {
        // Create provider token with adminId set
        Token providerToken = Token.builder()
                .tokenValue("PROVIDER-" + UUID.randomUUID())
                .role(Role.PROVIDER)
                .createdForEmail("provider@test.com")
                .expiryDate(LocalDateTime.now().plusYears(1))
                .isUsed(false)
                .createdByAdminId(adminId)
                .build();
        tokenRepository.save(providerToken);

        // Register provider
        RegisterRequest providerReq = new RegisterRequest();
        providerReq.setName("Provider");
        providerReq.setEmail("provider@test.com");
        providerReq.setPassword("ProvPass123");
        providerReq.setPhoneNumber("2222222222");
        providerReq.setRole(Role.PROVIDER);
        providerReq.setToken(providerToken.getTokenValue());
        providerReq.setPlaceId(placeId);
        ResponseEntity<String> regRes = restTemplate.postForEntity("/api/auth/register", providerReq, String.class);
        assertThat(regRes.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Manually verify provider
        User provider = userRepository.findByEmail("provider@test.com").orElseThrow();
        provider.setIsVerified(true);
        userRepository.save(provider);

        // Admin creates the service
        HttpHeaders adminHeaders = new HttpHeaders();
        adminHeaders.setBearerAuth(adminToken);
        adminHeaders.setContentType(MediaType.APPLICATION_JSON);

        ServiceDTO serviceDto = new ServiceDTO();
        serviceDto.setPlaceId(placeId);
        serviceDto.setName("Service");
        serviceDto.setAverageServiceTime(5);
        serviceDto.setIsActive(true);
        serviceDto.setSupportsGroupToken(false);      // <-- add this
        serviceDto.setEmergencySupport(false);        // <-- add this
        HttpEntity<ServiceDTO> serviceRequest = new HttpEntity<>(serviceDto, adminHeaders);
        ResponseEntity<ServiceDTO> serviceRes = restTemplate.postForEntity("/api/services", serviceRequest, ServiceDTO.class);
        assertThat(serviceRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        String serviceId = serviceRes.getBody().getId();

        // Provider creates the queue
        LoginRequest loginReq = new LoginRequest("provider@test.com", "ProvPass123");
        ResponseEntity<JwtResponse> loginRes = restTemplate.postForEntity("/api/auth/login", loginReq, JwtResponse.class);
        assertThat(loginRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        String providerJwt = loginRes.getBody().getToken();

        HttpHeaders providerHeaders = new HttpHeaders();
        providerHeaders.setBearerAuth(providerJwt);
        providerHeaders.setContentType(MediaType.APPLICATION_JSON);

        CreateQueueRequest queueReq = new CreateQueueRequest();
        queueReq.setServiceName("Test Queue");
        queueReq.setPlaceId(placeId);
        queueReq.setServiceId(serviceId);
        queueReq.setMaxCapacity(10);
        HttpEntity<CreateQueueRequest> queueRequest = new HttpEntity<>(queueReq, providerHeaders);
        ResponseEntity<Queue> queueRes = restTemplate.postForEntity("/api/queues/create", queueRequest, Queue.class);
        assertThat(queueRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return queueRes.getBody().getId();
    }

    private String createUser() {
        RegisterRequest userReq = new RegisterRequest();
        userReq.setName("User");
        userReq.setEmail("user@test.com");
        userReq.setPassword("UserPass123");
        userReq.setPhoneNumber("3333333333");
        userReq.setRole(Role.USER);
        ResponseEntity<String> regRes = restTemplate.postForEntity("/api/auth/register", userReq, String.class);
        assertThat(regRes.getStatusCode()).isEqualTo(HttpStatus.OK);

        User user = userRepository.findByEmail("user@test.com").orElseThrow();
        user.setIsVerified(true);
        userRepository.save(user);
        return user.getId();
    }

    private String loginUser() {
        LoginRequest loginReq = new LoginRequest("user@test.com", "UserPass123");
        ResponseEntity<JwtResponse> loginRes = restTemplate.postForEntity("/api/auth/login", loginReq, JwtResponse.class);
        assertThat(loginRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        return loginRes.getBody().getToken();
    }

    // ==================== TESTS ====================

    @Test
    void testCreateAndGetNotificationPreference() {
        // User joins queue
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(userToken);
        HttpEntity<Void> joinRequest = new HttpEntity<>(headers);
        ResponseEntity<Void> joinRes = restTemplate.postForEntity("/api/queues/" + queueId + "/add-token", joinRequest, Void.class);
        assertThat(joinRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Create preference
        NotificationPreferenceDTO prefDto = new NotificationPreferenceDTO();
        prefDto.setNotifyBeforeMinutes(10);
        prefDto.setNotifyOnStatusChange(true);
        prefDto.setNotifyOnEmergencyApproval(false);
        prefDto.setEnabled(true);

        HttpEntity<NotificationPreferenceDTO> createRequest = new HttpEntity<>(prefDto, headers);
        ResponseEntity<NotificationPreferenceDTO> createRes = restTemplate.exchange(
                "/api/notifications/preferences/queue/" + queueId,
                HttpMethod.PUT,
                createRequest,
                NotificationPreferenceDTO.class
        );
        assertThat(createRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        NotificationPreferenceDTO created = createRes.getBody();
        assertThat(created.getUserId()).isEqualTo(userId);
        assertThat(created.getQueueId()).isEqualTo(queueId);
        assertThat(created.getNotifyBeforeMinutes()).isEqualTo(10);
        assertThat(created.getNotifyOnStatusChange()).isTrue();
        assertThat(created.getNotifyOnEmergencyApproval()).isFalse();
        assertThat(created.getEnabled()).isTrue();

        // Get preference
        ResponseEntity<NotificationPreferenceDTO> getRes = restTemplate.exchange(
                "/api/notifications/preferences/queue/" + queueId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                NotificationPreferenceDTO.class
        );
        assertThat(getRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getRes.getBody().getNotifyBeforeMinutes()).isEqualTo(10);
    }

    @Test
    void testUpdatePreference() {
        // User joins queue
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(userToken);
        HttpEntity<Void> joinRequest = new HttpEntity<>(headers);
        restTemplate.postForEntity("/api/queues/" + queueId + "/add-token", joinRequest, Void.class);

        // Create initial
        NotificationPreferenceDTO prefDto = new NotificationPreferenceDTO();
        prefDto.setNotifyBeforeMinutes(5);
        prefDto.setNotifyOnStatusChange(true);
        prefDto.setNotifyOnEmergencyApproval(true);
        prefDto.setEnabled(true);
        HttpEntity<NotificationPreferenceDTO> createRequest = new HttpEntity<>(prefDto, headers);
        restTemplate.exchange(
                "/api/notifications/preferences/queue/" + queueId,
                HttpMethod.PUT,
                createRequest,
                NotificationPreferenceDTO.class
        );

        // Update
        prefDto.setNotifyBeforeMinutes(15);
        prefDto.setNotifyOnStatusChange(false);
        prefDto.setEnabled(false);
        ResponseEntity<NotificationPreferenceDTO> updateRes = restTemplate.exchange(
                "/api/notifications/preferences/queue/" + queueId,
                HttpMethod.PUT,
                new HttpEntity<>(prefDto, headers),
                NotificationPreferenceDTO.class
        );
        assertThat(updateRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        NotificationPreferenceDTO updated = updateRes.getBody();
        assertThat(updated.getNotifyBeforeMinutes()).isEqualTo(15);
        assertThat(updated.getNotifyOnStatusChange()).isFalse();
        assertThat(updated.getEnabled()).isFalse();
    }

    @Test
    void  testDeletePreference() {
        // User joins queue
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(userToken);
        HttpEntity<Void> joinRequest = new HttpEntity<>(headers);
        restTemplate.postForEntity("/api/queues/" + queueId + "/add-token", joinRequest, Void.class);

        // Create
        NotificationPreferenceDTO prefDto = new NotificationPreferenceDTO();
        prefDto.setNotifyBeforeMinutes(5);
        prefDto.setNotifyOnStatusChange(true);
        prefDto.setNotifyOnEmergencyApproval(true);
        prefDto.setEnabled(true);
        HttpEntity<NotificationPreferenceDTO> createRequest = new HttpEntity<>(prefDto, headers);
        restTemplate.exchange(
                "/api/notifications/preferences/queue/" + queueId,
                HttpMethod.PUT,
                createRequest,
                NotificationPreferenceDTO.class
        );

        // Delete
        ResponseEntity<Void> deleteRes = restTemplate.exchange(
                "/api/notifications/preferences/queue/" + queueId,
                HttpMethod.DELETE,
                new HttpEntity<>(headers),
                Void.class
        );
        assertThat(deleteRes.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Get should return 404
        ResponseEntity<NotificationPreferenceDTO> getRes = restTemplate.exchange(
                "/api/notifications/preferences/queue/" + queueId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                NotificationPreferenceDTO.class
        );
        assertThat(getRes.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void testGetMyPreferences() {
        // User joins queue
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(userToken);
        HttpEntity<Void> joinRequest = new HttpEntity<>(headers);
        restTemplate.postForEntity("/api/queues/" + queueId + "/add-token", joinRequest, Void.class);

        // Create preference
        NotificationPreferenceDTO prefDto = new NotificationPreferenceDTO();
        prefDto.setNotifyBeforeMinutes(10);
        prefDto.setNotifyOnStatusChange(true);
        prefDto.setNotifyOnEmergencyApproval(false);
        prefDto.setEnabled(true);
        HttpEntity<NotificationPreferenceDTO> createRequest = new HttpEntity<>(prefDto, headers);
        restTemplate.exchange(
                "/api/notifications/preferences/queue/" + queueId,
                HttpMethod.PUT,
                createRequest,
                NotificationPreferenceDTO.class
        );

        // Get all for user
        ResponseEntity<NotificationPreferenceDTO[]> getRes = restTemplate.exchange(
                "/api/notifications/preferences/my",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                NotificationPreferenceDTO[].class
        );
        assertThat(getRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getRes.getBody()).hasSize(1);
        assertThat(getRes.getBody()[0].getQueueId()).isEqualTo(queueId);
    }
}