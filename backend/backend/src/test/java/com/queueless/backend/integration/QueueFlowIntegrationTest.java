package com.queueless.backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.queueless.backend.dto.*;
import com.queueless.backend.enums.Role;
import com.queueless.backend.model.Queue;
import com.queueless.backend.model.QueueToken;
import com.queueless.backend.model.Token;
import com.queueless.backend.repository.TokenRepository;
import com.queueless.backend.repository.UserRepository;
import com.queueless.backend.service.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class QueueFlowIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private com.google.firebase.auth.FirebaseAuth firebaseAuth;

    @MockitoBean
    private com.razorpay.RazorpayClient razorpayClient;

    @Mock
    private AuditLogService auditLogService;

    private String adminToken;
    private String adminId;
    private String providerToken;
    private String providerEmail;
    private String placeId;
    private String queueId;
    private String userToken;
    private String userId;

    @BeforeEach
    void setUp() {
        // Clean up before each test
        tokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void fullQueueFlow() throws Exception {
        // 1. Register a USER
        RegisterRequest userReq = new RegisterRequest();
        userReq.setName("Test User");
        userReq.setEmail("user@example.com");
        userReq.setPassword("Password123");
        userReq.setPhoneNumber("1234567890");
        userReq.setRole(Role.USER);
        ResponseEntity<String> userRegResponse = restTemplate.postForEntity("/api/auth/register", userReq, String.class);
        assertThat(userRegResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Auto‑verify the user (since USER needs email verification, we'll directly set verified in DB for test)
        var user = userRepository.findByEmail("user@example.com").orElseThrow();
        user.setIsVerified(true);
        userRepository.save(user);
        userId = user.getId();

        // 2. Login as USER to get token (we'll use for later, but first we need admin)
        // Instead, create an admin token for registration
        Token adminPurchaseToken = Token.builder()
                .tokenValue("ADMIN-" + UUID.randomUUID())
                .role(Role.ADMIN)
                .createdForEmail("admin@example.com")
                .expiryDate(LocalDateTime.now().plusYears(1))
                .isUsed(false)
                .build();
        tokenRepository.save(adminPurchaseToken);

        // 3. Register ADMIN
        RegisterRequest adminReq = new RegisterRequest();
        adminReq.setName("Admin User");
        adminReq.setEmail("admin@example.com");
        adminReq.setPassword("AdminPass123");
        adminReq.setPhoneNumber("9876543210");
        adminReq.setRole(Role.ADMIN);
        adminReq.setToken(adminPurchaseToken.getTokenValue());
        ResponseEntity<String> adminRegResponse = restTemplate.postForEntity("/api/auth/register", adminReq, String.class);
        assertThat(adminRegResponse.getStatusCode()).isEqualTo(HttpStatus.OK);


        // Manually verify admin
        var adminUser = userRepository.findByEmail("admin@example.com").orElseThrow();
        adminUser.setIsVerified(true);
        userRepository.save(adminUser);
        adminId = adminUser.getId();
        // 4. Login as ADMIN
        LoginRequest adminLogin = new LoginRequest("admin@example.com", "AdminPass123");
        ResponseEntity<JwtResponse> adminLoginResponse = restTemplate.postForEntity("/api/auth/login", adminLogin, JwtResponse.class);
        assertThat(adminLoginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        adminToken = adminLoginResponse.getBody().getToken();


        // 5. Create a place
        PlaceDTO placeDto = new PlaceDTO();
        placeDto.setName("Test Place");
        placeDto.setType("SHOP");
        placeDto.setAddress("123 Test St");
        placeDto.setLocation(new double[]{10.0, 20.0});
        placeDto.setAdminId(adminId);
        placeDto.setIsActive(true);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<PlaceDTO> placeRequest = new HttpEntity<>(placeDto, headers);
        ResponseEntity<PlaceDTO> placeResponse = restTemplate.postForEntity("/api/places", placeRequest, PlaceDTO.class);
        assertThat(placeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        placeId = placeResponse.getBody().getId();

        // 6. Create a service under the place
        ServiceDTO serviceDto = new ServiceDTO();
        serviceDto.setPlaceId(placeId);
        serviceDto.setName("Test Service");
        serviceDto.setDescription("A test service");
        serviceDto.setAverageServiceTime(5);
        serviceDto.setSupportsGroupToken(true);
        serviceDto.setEmergencySupport(false);
        serviceDto.setIsActive(true);

        HttpEntity<ServiceDTO> serviceRequest = new HttpEntity<>(serviceDto, headers);
        ResponseEntity<ServiceDTO> serviceResponse = restTemplate.postForEntity("/api/services", serviceRequest, ServiceDTO.class);
        assertThat(serviceResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String serviceId = serviceResponse.getBody().getId();

        // 7. Create a provider token (purchased by admin)
        Token providerPurchaseToken = Token.builder()
                .tokenValue("PROVIDER-" + UUID.randomUUID())
                .role(Role.PROVIDER)
                .createdForEmail("provider@example.com")
                .expiryDate(LocalDateTime.now().plusYears(1))
                .isUsed(false)
                .createdByAdminId(adminId)
                .build();
        tokenRepository.save(providerPurchaseToken);

        // 8. Register PROVIDER
        RegisterRequest providerReq = new RegisterRequest();
        providerReq.setName("Provider User");
        providerReq.setEmail("provider@example.com");
        providerReq.setPassword("ProvPass123");
        providerReq.setPhoneNumber("5551234567");
        providerReq.setRole(Role.PROVIDER);
        providerReq.setToken(providerPurchaseToken.getTokenValue());
        providerReq.setPlaceId(placeId);

        ResponseEntity<String> providerRegResponse = restTemplate.postForEntity("/api/auth/register", providerReq, String.class);
        assertThat(providerRegResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        providerEmail = "provider@example.com";

        // Manually verify provider
        var providerUser = userRepository.findByEmail("provider@example.com").orElseThrow();
        providerUser.setIsVerified(true);
        userRepository.save(providerUser);
        // 9. Login as PROVIDER
        LoginRequest providerLogin = new LoginRequest(providerEmail, "ProvPass123");
        ResponseEntity<JwtResponse> providerLoginResponse = restTemplate.postForEntity("/api/auth/login", providerLogin, JwtResponse.class);
        assertThat(providerLoginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        providerToken = providerLoginResponse.getBody().getToken();

        // 10. Provider creates a queue
        CreateQueueRequest queueReq = new CreateQueueRequest();
        queueReq.setServiceName("Test Queue");
        queueReq.setPlaceId(placeId);
        queueReq.setServiceId(serviceId);
        queueReq.setMaxCapacity(10);
        queueReq.setSupportsGroupToken(true);
        queueReq.setEmergencySupport(false);
        queueReq.setEmergencyPriorityWeight(5);

        HttpHeaders providerHeaders = new HttpHeaders();
        providerHeaders.setBearerAuth(providerToken);
        providerHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateQueueRequest> queueRequest = new HttpEntity<>(queueReq, providerHeaders);
        ResponseEntity<Queue> queueResponse = restTemplate.postForEntity("/api/queues/create", queueRequest, Queue.class);
        assertThat(queueResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        queueId = queueResponse.getBody().getId();

        // 11. User logs in
        LoginRequest userLogin = new LoginRequest("user@example.com", "Password123");
        ResponseEntity<JwtResponse> userLoginResponse = restTemplate.postForEntity("/api/auth/login", userLogin, JwtResponse.class);
        assertThat(userLoginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        userToken = userLoginResponse.getBody().getToken();

        // 12. User joins the queue
        HttpHeaders userHeaders = new HttpHeaders();
        userHeaders.setBearerAuth(userToken);
        HttpEntity<Void> joinRequest = new HttpEntity<>(userHeaders);
        ResponseEntity<QueueToken> joinResponse = restTemplate.postForEntity("/api/queues/" + queueId + "/add-token", joinRequest, QueueToken.class);
        assertThat(joinResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String tokenId = joinResponse.getBody().getTokenId();

        // 13. Provider serves next token
        HttpHeaders serveHeaders = new HttpHeaders();
        serveHeaders.setBearerAuth(providerToken);
        HttpEntity<Void> serveRequest = new HttpEntity<>(serveHeaders);
        ResponseEntity<Queue> serveResponse = restTemplate.postForEntity("/api/queues/" + queueId + "/serve-next", serveRequest, Queue.class);
        assertThat(serveResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 14. Provider completes the token
        TokenRequest completeReq = new TokenRequest();
        completeReq.setTokenId(tokenId);
        HttpEntity<TokenRequest> completeRequest = new HttpEntity<>(completeReq, providerHeaders);
        ResponseEntity<Queue> completeResponse = restTemplate.postForEntity("/api/queues/" + queueId + "/complete-token", completeRequest, Queue.class);
        assertThat(completeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 15. User views token history
        HttpHeaders userHistoryHeaders = new HttpHeaders();
        userHistoryHeaders.setBearerAuth(userToken);
        HttpEntity<Void> historyRequest = new HttpEntity<>(userHistoryHeaders);
        String url = UriComponentsBuilder.fromPath("/api/user/tokens")
                .queryParam("days", 30)
                .queryParam("page", 0)
                .queryParam("size", 20)
                .build().toUriString();
        ResponseEntity<UserTokenHistoryDTO[]> historyResponse = restTemplate.exchange(
                url, HttpMethod.GET, historyRequest, UserTokenHistoryDTO[].class);
        assertThat(historyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(historyResponse.getBody()).hasSize(1);
        assertThat(historyResponse.getBody()[0].getStatus()).isEqualTo("COMPLETED");
        assertThat(historyResponse.getBody()[0].getTokenId()).isEqualTo(tokenId);

        // 16. User checks position (should be null now, as token completed)
        ResponseEntity<UserPositionDTO> positionResponse = restTemplate.exchange(
                "/api/queues/" + queueId + "/position/" + userId,
                HttpMethod.GET,
                new HttpEntity<>(userHeaders),
                UserPositionDTO.class);
        assertThat(positionResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(positionResponse.getBody().getPosition()).isNull();
    }
}