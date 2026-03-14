package com.queueless.backend.service;

import com.queueless.backend.dto.PlaceDTO;
import com.queueless.backend.model.Place;
import com.queueless.backend.repository.PlaceRepository;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlaceServiceTest {

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private PlaceService placeService;

    private Place testPlace;
    private final String placeId = new ObjectId().toHexString();
    private final String adminId = "507f191e810c19729de860ea";
    @BeforeEach
    void setUp() {
        testPlace = new Place();
        testPlace.setId(placeId);
        testPlace.setName("Test Place");
        testPlace.setType("SHOP");
        testPlace.setAddress("123 Main St");
        testPlace.setLocation(new GeoJsonPoint(10.0, 20.0));
        testPlace.setAdminId(adminId);
        testPlace.setImageUrls(List.of("img1.jpg", "img2.jpg"));
        testPlace.setDescription("A test place");
        testPlace.setRating(4.5);
        testPlace.setTotalRatings(10);
        testPlace.setContactInfo(Map.of("phone", "123-456-7890"));
        testPlace.setBusinessHours(new ArrayList<>());
        testPlace.setIsActive(true);
    }

    // ================= CREATE PLACE =================

    @Test
    void createPlaceSuccess() {
        PlaceDTO dto = PlaceDTO.fromEntity(testPlace);
        when(placeRepository.save(any(Place.class))).thenReturn(testPlace);

        Place created = placeService.createPlace(dto);

        assertNotNull(created);
        assertEquals(testPlace.getId(), created.getId());
        assertEquals(testPlace.getName(), created.getName());
        assertEquals(testPlace.getType(), created.getType());
        assertEquals(testPlace.getAddress(), created.getAddress());
        assertEquals(testPlace.getLocation().getX(), created.getLocation().getX());
        assertEquals(testPlace.getLocation().getY(), created.getLocation().getY());
        assertEquals(testPlace.getAdminId(), created.getAdminId());
        assertEquals(testPlace.getImageUrls(), created.getImageUrls());
        assertEquals(testPlace.getDescription(), created.getDescription());
        assertEquals(testPlace.getRating(), created.getRating());
        assertEquals(testPlace.getTotalRatings(), created.getTotalRatings());
        assertEquals(testPlace.getContactInfo(), created.getContactInfo());
        assertEquals(testPlace.getBusinessHours(), created.getBusinessHours());
        assertEquals(testPlace.getIsActive(), created.getIsActive());

        verify(placeRepository).save(any(Place.class));
    }

    @Test
    void createPlaceWithNullLocationDefaultsToNull() {
        PlaceDTO dto = PlaceDTO.fromEntity(testPlace);
        dto.setLocation(null); // remove location
        when(placeRepository.save(any(Place.class))).thenAnswer(inv -> inv.getArgument(0));

        Place created = placeService.createPlace(dto);

        assertNull(created.getLocation());
        verify(placeRepository).save(any(Place.class));
    }

    // ================= GET PLACE BY ID =================

    @Test
    void getPlaceByIdSuccess() {
        when(placeRepository.findById(placeId)).thenReturn(Optional.of(testPlace));

        Place found = placeService.getPlaceById(placeId);

        assertNotNull(found);
        assertEquals(placeId, found.getId());
        verify(placeRepository).findById(placeId);
    }

    @Test
    void getPlaceByIdNotFound() {
        when(placeRepository.findById(placeId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> placeService.getPlaceById(placeId));
        assertEquals("Place not found with id: " + placeId, exception.getMessage());
        verify(placeRepository).findById(placeId);
    }

    // ================= GET PLACES BY ADMIN ID =================

    @Test
    void getPlacesByAdminIdSuccess() {
        List<Place> places = List.of(testPlace);
        when(placeRepository.findByAdminId(adminId)).thenReturn(places);

        List<Place> result = placeService.getPlacesByAdminId(adminId);

        assertEquals(1, result.size());
        assertEquals(testPlace.getId(), result.get(0).getId());
        verify(placeRepository).findByAdminId(adminId);
    }

    @Test
    void getPlacesByAdminIdEmpty() {
        when(placeRepository.findByAdminId(adminId)).thenReturn(List.of());

        List<Place> result = placeService.getPlacesByAdminId(adminId);

        assertTrue(result.isEmpty());
        verify(placeRepository).findByAdminId(adminId);
    }

    // ================= IS PLACE OWNED BY ADMIN =================

    @Test
    void isPlaceOwnedByAdminTrue() {
        when(placeRepository.findById(placeId)).thenReturn(Optional.of(testPlace));

        boolean result = placeService.isPlaceOwnedByAdmin(placeId, adminId);

        assertTrue(result);
        verify(placeRepository).findById(placeId);
    }

    @Test
    void isPlaceOwnedByAdminFalse() {
        when(placeRepository.findById(placeId)).thenReturn(Optional.of(testPlace));

        boolean result = placeService.isPlaceOwnedByAdmin(placeId, "otherAdmin");

        assertFalse(result);
        verify(placeRepository).findById(placeId);
    }

    @Test
    void isPlaceOwnedByAdminPlaceNotFound() {
        when(placeRepository.findById(placeId)).thenReturn(Optional.empty());

        boolean result = placeService.isPlaceOwnedByAdmin(placeId, adminId);

        assertFalse(result);
        verify(placeRepository).findById(placeId);
    }

    // ================= GET PLACES BY TYPE =================

    @Test
    void getPlacesByTypeSuccess() {
        List<Place> places = List.of(testPlace);
        when(placeRepository.findByType("SHOP")).thenReturn(places);

        List<Place> result = placeService.getPlacesByType("SHOP");

        assertEquals(1, result.size());
        verify(placeRepository).findByType("SHOP");
    }

    // ================= GET NEARBY PLACES =================

    @Test
    void getNearbyPlacesSuccess() {
        List<Place> places = List.of(testPlace);
        double lon = 10.0, lat = 20.0, radius = 5.0;
        double maxDistance = radius * 1000;
        when(placeRepository.findByLocationNear(lon, lat, maxDistance)).thenReturn(places);

        List<Place> result = placeService.getNearbyPlaces(lon, lat, radius);

        assertEquals(1, result.size());
        verify(placeRepository).findByLocationNear(lon, lat, maxDistance);
    }

    // ================= UPDATE PLACE =================

    @Test
    void updatePlaceSuccess() {
        PlaceDTO updateDto = new PlaceDTO();
        updateDto.setName("Updated Name");
        updateDto.setDescription("Updated description");
        updateDto.setRating(4.8);
        updateDto.setIsActive(false);

        when(placeRepository.findById(placeId)).thenReturn(Optional.of(testPlace));
        when(placeRepository.save(any(Place.class))).thenAnswer(inv -> inv.getArgument(0));

        Place updated = placeService.updatePlace(placeId, updateDto);

        assertEquals("Updated Name", updated.getName());
        assertEquals("Updated description", updated.getDescription());
        assertEquals(4.8, updated.getRating());
        assertFalse(updated.getIsActive());
        // unchanged fields
        assertEquals(testPlace.getType(), updated.getType());
        assertEquals(testPlace.getAddress(), updated.getAddress());

        verify(placeRepository).findById(placeId);
        verify(placeRepository).save(testPlace);
    }

    @Test
    void updatePlaceNotFound() {
        when(placeRepository.findById(placeId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> placeService.updatePlace(placeId, new PlaceDTO()));
        assertEquals("Place not found with id: " + placeId, exception.getMessage());
        verify(placeRepository).findById(placeId);
        verifyNoMoreInteractions(placeRepository);
    }

    // ================= DELETE PLACE =================

    @Test
    void deletePlaceSuccess() {
        when(placeRepository.findById(placeId)).thenReturn(Optional.of(testPlace));
        doNothing().when(placeRepository).delete(testPlace);

        assertDoesNotThrow(() -> placeService.deletePlace(placeId));

        verify(placeRepository).findById(placeId);
        verify(placeRepository).delete(testPlace);
    }

    @Test
    void deletePlaceNotFound() {
        when(placeRepository.findById(placeId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> placeService.deletePlace(placeId));
        assertEquals("Place not found with id: " + placeId, exception.getMessage());
        verify(placeRepository).findById(placeId);
        verifyNoMoreInteractions(placeRepository);
    }

    // ================= GET ALL PLACES =================

    @Test
    void getAllPlacesSuccess() {
        List<Place> places = List.of(testPlace);
        when(placeRepository.findAll()).thenReturn(places);

        List<Place> result = placeService.getAllPlaces();

        assertEquals(1, result.size());
        verify(placeRepository).findAll();
    }

    // ================= GET PLACES BY IDS =================

    @Test
    void getPlacesByIdsSuccess() {
        List<String> ids = List.of(placeId);
        List<ObjectId> objectIds = ids.stream().map(ObjectId::new).toList();
        List<Place> expected = List.of(testPlace);
        when(placeRepository.findFavoritesByIdIn(objectIds)).thenReturn(expected);

        List<Place> result = placeService.getPlacesByIds(ids);

        assertEquals(1, result.size());
        verify(placeRepository).findFavoritesByIdIn(objectIds);
    }

    @Test
    void getPlacesByIdsEmptyInput() {
        List<Place> result = placeService.getPlacesByIds(List.of());
        assertTrue(result.isEmpty());
        verifyNoInteractions(placeRepository);
    }
}