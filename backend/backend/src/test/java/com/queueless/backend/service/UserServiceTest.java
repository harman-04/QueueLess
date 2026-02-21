package com.queueless.backend.service;

import com.queueless.backend.dto.PasswordChangeRequest;
import com.queueless.backend.dto.UserProfileUpdateRequest;
import com.queueless.backend.model.User;
import com.queueless.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private final String userId = "user123";

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(userId)
                .name("John Doe")
                .email("john@example.com")
                .password("encodedCurrentPassword")
                .phoneNumber("1234567890")
                .role(com.queueless.backend.enums.Role.USER)
                .isVerified(true)
                .preferences(User.UserPreferences.builder()
                        .emailNotifications(true)
                        .smsNotifications(false)
                        .language("en")
                        .defaultSearchRadius(5)
                        .darkMode(false)
                        .favoritePlaceIds(new ArrayList<>())
                        .build())
                .build();
    }

    // ================= UPDATE PROFILE =================

    @Test
    void updateUserProfileSuccess() {
        UserProfileUpdateRequest request = new UserProfileUpdateRequest();
        request.setName("Jane Doe");
        request.setPhoneNumber("9876543210");
        request.setProfileImageUrl("https://example.com/avatar.jpg");

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> userService.updateUserProfile(userId, request));

        verify(userRepository).findById(userId);
        verify(userRepository).save(argThat(user -> {
            assertEquals("Jane Doe", user.getName());
            assertEquals("9876543210", user.getPhoneNumber());
            assertEquals("https://example.com/avatar.jpg", user.getProfileImageUrl());
            return true;
        }));
    }

    @Test
    void updateUserProfilePartialUpdate() {
        UserProfileUpdateRequest request = new UserProfileUpdateRequest();
        request.setName("Jane Doe");
        // phoneNumber and profileImageUrl remain null

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.updateUserProfile(userId, request);

        verify(userRepository).save(argThat(user -> {
            assertEquals("Jane Doe", user.getName());
            assertEquals("1234567890", user.getPhoneNumber()); // unchanged
            assertNull(user.getProfileImageUrl()); // unchanged
            return true;
        }));
    }

    @Test
    void updateUserProfileUserNotFound() {
        UserProfileUpdateRequest request = new UserProfileUpdateRequest();
        request.setName("Jane Doe");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.updateUserProfile(userId, request));
        assertEquals("User not found", exception.getMessage());

        verify(userRepository).findById(userId);
        verifyNoMoreInteractions(userRepository);
    }

    // ================= CHANGE PASSWORD =================

    @Test
    void changePasswordSuccess() {
        // 1. Arrange
        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setCurrentPassword("oldPassword");
        request.setNewPassword("NewPassword123");

        // Store the initial password to use for verification
        String initialEncodedPassword = testUser.getPassword();

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPassword", initialEncodedPassword)).thenReturn(true);
        when(passwordEncoder.encode("NewPassword123")).thenReturn("encodedNewPassword");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        // 2. Act
        assertDoesNotThrow(() -> userService.changePassword(userId, request));

        // 3. Verify
        verify(userRepository).findById(userId);

        // FIX: Verify against the value that WAS there during the call, not the current state
        verify(passwordEncoder).matches("oldPassword", "encodedCurrentPassword");

        verify(passwordEncoder).encode("NewPassword123");
        verify(userRepository).save(argThat(user ->
                user.getPassword().equals("encodedNewPassword")
        ));
    }

    @Test
    void changePasswordIncorrectCurrent() {
        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setCurrentPassword("wrongPassword");
        request.setNewPassword("NewPassword123");

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPassword", testUser.getPassword())).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.changePassword(userId, request));
        assertEquals("Incorrect current password", exception.getMessage());

        verify(userRepository).findById(userId);
        verify(passwordEncoder).matches("wrongPassword", testUser.getPassword());
        verifyNoMoreInteractions(passwordEncoder, userRepository);
    }

    @Test
    void changePasswordUserNotFound() {
        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setCurrentPassword("oldPassword");
        request.setNewPassword("NewPassword123");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.changePassword(userId, request));
        assertEquals("User not found", exception.getMessage());
    }

    // ================= DELETE ACCOUNT =================

    @Test
    void deleteAccountSuccess() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        assertDoesNotThrow(() -> userService.deleteAccount(userId));

        verify(userRepository).findById(userId);
        verify(userRepository).delete(testUser);
    }

    @Test
    void deleteAccountUserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.deleteAccount(userId));
        assertEquals("User not found", exception.getMessage());

        verify(userRepository).findById(userId);
        verifyNoMoreInteractions(userRepository);
    }

    // ================= FAVORITE PLACES =================

    @Test
    void getFavoritePlacesSuccess() {
        List<String> favorites = List.of("place1", "place2");
        testUser.getPreferences().setFavoritePlaceIds(new ArrayList<>(favorites));

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        List<String> result = userService.getFavoritePlaces(userId);

        assertEquals(favorites, result);
        verify(userRepository).findById(userId);
    }

    @Test
    void getFavoritePlacesNoPreferences() {
        testUser.setPreferences(null); // no preferences object
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        List<String> result = userService.getFavoritePlaces(userId);

        assertTrue(result.isEmpty());
        verify(userRepository).findById(userId);
    }

    @Test
    void getFavoritePlacesNullList() {
        testUser.getPreferences().setFavoritePlaceIds(null); // null list inside preferences
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        List<String> result = userService.getFavoritePlaces(userId);

        assertTrue(result.isEmpty());
        verify(userRepository).findById(userId);
    }

    @Test
    void getFavoritePlacesUserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.getFavoritePlaces(userId));
        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void addFavoritePlaceSuccess() {
        String placeId = "place123";
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> userService.addFavoritePlace(userId, placeId));

        verify(userRepository).findById(userId);
        verify(userRepository).save(argThat(user ->
                user.getPreferences().getFavoritePlaceIds().contains(placeId) &&
                        user.getPreferences().getFavoritePlaceIds().size() == 1
        ));
    }

    @Test
    void addFavoritePlaceAlreadyExists() {
        String placeId = "place123";
        testUser.getPreferences().getFavoritePlaceIds().add(placeId); // already present

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        // No save expected because already present

        assertDoesNotThrow(() -> userService.addFavoritePlace(userId, placeId));

        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any());
    }

    @Test
    void addFavoritePlaceInitializePreferences() {
        String placeId = "place123";
        testUser.setPreferences(null); // no preferences

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> userService.addFavoritePlace(userId, placeId));

        verify(userRepository).findById(userId);
        verify(userRepository).save(argThat(user -> {
            assertNotNull(user.getPreferences());
            assertNotNull(user.getPreferences().getFavoritePlaceIds());
            assertTrue(user.getPreferences().getFavoritePlaceIds().contains(placeId));
            return true;
        }));
    }

    @Test
    void addFavoritePlaceUserNotFound() {
        String placeId = "place123";
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.addFavoritePlace(userId, placeId));
        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void removeFavoritePlaceSuccess() {
        String placeId = "place123";
        testUser.getPreferences().getFavoritePlaceIds().add(placeId);
        testUser.getPreferences().getFavoritePlaceIds().add("place456");

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> userService.removeFavoritePlace(userId, placeId));

        verify(userRepository).findById(userId);
        verify(userRepository).save(argThat(user ->
                !user.getPreferences().getFavoritePlaceIds().contains(placeId) &&
                        user.getPreferences().getFavoritePlaceIds().size() == 1 &&
                        user.getPreferences().getFavoritePlaceIds().contains("place456")
        ));
    }

    @Test
    void removeFavoritePlaceNotExists() {
        String placeId = "place123"; // not in favorites
        testUser.getPreferences().getFavoritePlaceIds().add("place456");

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        // No save expected because place not found

        assertDoesNotThrow(() -> userService.removeFavoritePlace(userId, placeId));

        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any());
    }

    @Test
    void removeFavoritePlaceNoPreferences() {
        String placeId = "place123";
        testUser.setPreferences(null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        assertDoesNotThrow(() -> userService.removeFavoritePlace(userId, placeId));

        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any());
    }

    @Test
    void removeFavoritePlaceUserNotFound() {
        String placeId = "place123";
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.removeFavoritePlace(userId, placeId));
        assertEquals("User not found", exception.getMessage());
    }
}