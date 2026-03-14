package com.queueless.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.queueless.backend.dto.PasswordChangeRequest;
import com.queueless.backend.dto.PlaceDTO;
import com.queueless.backend.dto.UserProfileUpdateRequest;
import com.queueless.backend.dto.UserTokenHistoryDTO;
import com.queueless.backend.model.Place;
import com.queueless.backend.model.User;
import com.queueless.backend.service.FileStorageService;
import com.queueless.backend.service.PlaceService;
import com.queueless.backend.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.cache.type=none",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration"
})
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private PlaceService placeService;

    @MockitoBean
    private FileStorageService fileStorageService;

    @Autowired
    private ObjectMapper objectMapper;

    private final String userId = "user123";
    private final String placeId = "place123";

    // ==================== UPDATE PROFILE ====================

    @Test
    @WithMockUser(username = userId)
    void updateUserProfile_Success() throws Exception {
        UserProfileUpdateRequest request = new UserProfileUpdateRequest();
        request.setName("New Name");
        request.setPhoneNumber("9876543210");
        request.setProfileImageUrl("https://example.com/image.jpg");

        doNothing().when(userService).updateUserProfile(eq(userId), any(UserProfileUpdateRequest.class));

        mockMvc.perform(put("/api/user/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Profile updated successfully"));
    }

    @Test
    @WithMockUser(username = userId)
    void updateUserProfile_ServiceException() throws Exception {
        UserProfileUpdateRequest request = new UserProfileUpdateRequest();
        request.setName("New Name");

        doThrow(new RuntimeException("User not found"))
                .when(userService).updateUserProfile(eq(userId), any(UserProfileUpdateRequest.class));

        mockMvc.perform(put("/api/user/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("User not found"));
    }

    // ==================== CHANGE PASSWORD ====================

    @Test
    @WithMockUser(username = userId)
    void changePassword_Success() throws Exception {
        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setCurrentPassword("oldPass");
        request.setNewPassword("newPass123");

        doNothing().when(userService).changePassword(eq(userId), any(PasswordChangeRequest.class));

        mockMvc.perform(put("/api/user/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Password changed successfully"));
    }

    @Test
    @WithMockUser(username = userId)
    void changePassword_ServiceException() throws Exception {
        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setCurrentPassword("wrong");
        request.setNewPassword("newPass123");

        doThrow(new RuntimeException("Incorrect current password"))
                .when(userService).changePassword(eq(userId), any(PasswordChangeRequest.class));

        mockMvc.perform(put("/api/user/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Incorrect current password"));
    }

    // ==================== DELETE ACCOUNT ====================

    @Test
    @WithMockUser(username = userId)
    void deleteAccount_Success() throws Exception {
        doNothing().when(userService).deleteAccount(userId);

        mockMvc.perform(delete("/api/user/account"))
                .andExpect(status().isOk())
                .andExpect(content().string("Account deleted successfully"));
    }

    @Test
    @WithMockUser(username = userId)
    void deleteAccount_ServiceException() throws Exception {
        doThrow(new RuntimeException("Delete failed"))
                .when(userService).deleteAccount(userId);

        mockMvc.perform(delete("/api/user/account"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Failed to delete account."));
    }

    // ==================== GET FAVORITE PLACE IDs ====================

    @Test
    @WithMockUser(username = userId)
    void getFavoritePlaces_Success() throws Exception {
        List<String> favoriteIds = List.of("place1", "place2");
        when(userService.getFavoritePlaces(userId)).thenReturn(favoriteIds);

        mockMvc.perform(get("/api/user/favorites"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("place1"))
                .andExpect(jsonPath("$[1]").value("place2"));
    }

    @Test
    @WithMockUser(username = userId)
    void getFavoritePlaces_ServiceException() throws Exception {
        when(userService.getFavoritePlaces(userId)).thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(get("/api/user/favorites"))
                .andExpect(status().isInternalServerError());
    }

    // ==================== ADD FAVORITE PLACE ====================

    @Test
    @WithMockUser(username = userId)
    void addFavoritePlace_Success() throws Exception {
        doNothing().when(userService).addFavoritePlace(userId, placeId);

        mockMvc.perform(post("/api/user/favorites/{placeId}", placeId))
                .andExpect(status().isOk())
                .andExpect(content().string("Place added to favorites"));
    }

    @Test
    @WithMockUser(username = userId)
    void addFavoritePlace_ServiceException() throws Exception {
        doThrow(new RuntimeException("Add failed"))
                .when(userService).addFavoritePlace(userId, placeId);

        mockMvc.perform(post("/api/user/favorites/{placeId}", placeId))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Failed to add favorite place"));
    }

    // ==================== REMOVE FAVORITE PLACE ====================

    @Test
    @WithMockUser(username = userId)
    void removeFavoritePlace_Success() throws Exception {
        doNothing().when(userService).removeFavoritePlace(userId, placeId);

        mockMvc.perform(delete("/api/user/favorites/{placeId}", placeId))
                .andExpect(status().isOk())
                .andExpect(content().string("Place removed from favorites"));
    }

    @Test
    @WithMockUser(username = userId)
    void removeFavoritePlace_ServiceException() throws Exception {
        doThrow(new RuntimeException("Remove failed"))
                .when(userService).removeFavoritePlace(userId, placeId);

        mockMvc.perform(delete("/api/user/favorites/{placeId}", placeId))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Failed to remove favorite place"));
    }

    // ==================== GET FAVORITE PLACES WITH DETAILS ====================

    @Test
    @WithMockUser(username = userId)
    void getFavoritePlacesWithDetails_Success() throws Exception {
        List<String> favoriteIds = List.of(placeId);
        when(userService.getFavoritePlaces(userId)).thenReturn(favoriteIds);

        Place place = new Place();
        place.setId(placeId);
        place.setName("Test Place");
        place.setType("SHOP");
        place.setAddress("123 Main St");
        place.setLocation(new GeoJsonPoint(10.0, 20.0));
        place.setAdminId("admin123");
        place.setIsActive(true);
        when(placeService.getPlacesByIds(favoriteIds)).thenReturn(List.of(place));

        mockMvc.perform(get("/api/user/favorites/details"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(placeId))
                .andExpect(jsonPath("$[0].name").value("Test Place"));
    }

    @Test
    @WithMockUser(username = userId)
    void getFavoritePlacesWithDetails_ServiceException() throws Exception {
        when(userService.getFavoritePlaces(userId)).thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(get("/api/user/favorites/details"))
                .andExpect(status().isInternalServerError());
    }

    // ==================== GET USER TOKEN HISTORY (ANALYTICS) ====================

    @Test
    @WithMockUser(username = userId)
    void getUserTokenHistory_Analytics_Success() throws Exception {
        Map<String, Object> data = Map.of(
                "dates", List.of("2026-03-01", "2026-03-02"),
                "counts", List.of(3, 5)
        );
        when(userService.getUserTokenHistory(userId, 30)).thenReturn(data);

        mockMvc.perform(get("/api/user/analytics/token-history")
                        .param("days", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dates[0]").value("2026-03-01"))
                .andExpect(jsonPath("$.counts[1]").value(5));
    }

    // ==================== REGISTER FCM TOKEN ====================

    @Test
    @WithMockUser(username = userId)
    void registerFcmToken_Success() throws Exception {
        String fcmToken = "fcm-token-123";
        doNothing().when(userService).addFcmToken(userId, fcmToken);

        mockMvc.perform(post("/api/user/fcm-token")
                        .param("token", fcmToken))
                .andExpect(status().isOk())
                .andExpect(content().string("Token registered"));
    }

    // ==================== UNREGISTER FCM TOKEN ====================

    @Test
    @WithMockUser(username = userId)
    void unregisterFcmToken_Success() throws Exception {
        String fcmToken = "fcm-token-123";
        doNothing().when(userService).removeFcmToken(userId, fcmToken);

        mockMvc.perform(delete("/api/user/fcm-token")
                        .param("token", fcmToken))
                .andExpect(status().isOk())
                .andExpect(content().string("Token removed"));
    }

    // ==================== GET USER TOKEN HISTORY (PAGINATED) ====================

    @Test
    @WithMockUser(username = userId)
    void getUserTokenHistory_Paginated_Success() throws Exception {
        UserTokenHistoryDTO dto1 = new UserTokenHistoryDTO();
        dto1.setTokenId("token1");
        dto1.setServiceName("Service1");

        UserTokenHistoryDTO dto2 = new UserTokenHistoryDTO();
        dto2.setTokenId("token2");
        dto2.setServiceName("Service2");

        List<UserTokenHistoryDTO> history = List.of(dto1, dto2);
        when(userService.getUserTokenHistoryOptimized(eq(userId), eq(30), any(Pageable.class)))
                .thenReturn(history);

        mockMvc.perform(get("/api/user/tokens")
                        .param("days", "30")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tokenId").value("token1"))
                .andExpect(jsonPath("$[1].tokenId").value("token2"));
    }

    @Test
    @WithMockUser(username = userId)
    void uploadProfileImage_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        String imageUrl = "/uploads/test.jpg";
        when(fileStorageService.storeFile(any(MultipartFile.class), eq(userId))).thenReturn(imageUrl);
        doNothing().when(userService).updateProfileImage(userId, imageUrl);

        mockMvc.perform(multipart("/api/user/profile/image")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl").value(imageUrl));
    }
}