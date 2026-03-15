package com.queueless.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.queueless.backend.config.RateLimitConfig;
import com.queueless.backend.config.TestSecurityConfig;
import com.queueless.backend.dto.PlaceDTO;
import com.queueless.backend.model.Place;
import com.queueless.backend.service.PlaceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PlaceController.class,
        properties = "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration")
@Import({RateLimitConfig.class, TestSecurityConfig.class})
@AutoConfigureMockMvc
class PlaceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlaceService placeService;

    @Autowired
    private ObjectMapper objectMapper;

    private final String adminId = "admin123";
    private final String placeId = "place123";
    private final Place testPlace;

    PlaceControllerTest() {
        testPlace = new Place();
        testPlace.setId(placeId);
        testPlace.setName("Test Place");
        testPlace.setType("SHOP");
        testPlace.setAddress("123 Main St");
        testPlace.setLocation(new GeoJsonPoint(10.0, 20.0));
        testPlace.setAdminId(adminId);
        testPlace.setIsActive(true);
    }

    // ==================== CREATE PLACE ====================

    @Test
    @WithMockUser(username = adminId, roles = {"ADMIN"})
    void createPlace_Success() throws Exception {
        PlaceDTO request = PlaceDTO.fromEntity(testPlace);
        request.setAdminId(adminId);

        when(placeService.createPlace(any(PlaceDTO.class))).thenReturn(testPlace);

        mockMvc.perform(post("/api/places")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(placeId))
                .andExpect(jsonPath("$.name").value("Test Place"));
    }

    @Test
    @WithMockUser(username = adminId, roles = {"ADMIN"})
    void createPlace_AdminIdMismatch() throws Exception {
        PlaceDTO request = PlaceDTO.fromEntity(testPlace);
        request.setAdminId("otherAdmin"); // mismatched adminId

        mockMvc.perform(post("/api/places")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = adminId, roles = {"ADMIN"})
    void createPlace_ServiceException() throws Exception {
        PlaceDTO request = PlaceDTO.fromEntity(testPlace);
        request.setAdminId(adminId);

        when(placeService.createPlace(any(PlaceDTO.class)))
                .thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(post("/api/places")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }

    // ==================== GET PLACE BY ID ====================

    @Test
    void getPlace_Success() throws Exception {
        when(placeService.getPlaceById(placeId)).thenReturn(testPlace);

        mockMvc.perform(get("/api/places/{id}", placeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(placeId));
    }

    @Test
    void getPlace_NotFound() throws Exception {
        when(placeService.getPlaceById(placeId)).thenThrow(new RuntimeException("Not found"));

        mockMvc.perform(get("/api/places/{id}", placeId))
                .andExpect(status().isNotFound());
    }

    // ==================== GET MY PLACES ====================

    @Test
    @WithMockUser(username = adminId, roles = {"ADMIN"})
    void getMyPlaces_Success() throws Exception {
        when(placeService.getPlacesByAdminId(adminId)).thenReturn(List.of(testPlace));

        mockMvc.perform(get("/api/places/admin/my-places"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(placeId));
    }

    @Test
    @WithMockUser(username = adminId, roles = {"ADMIN"})
    void getMyPlaces_Empty() throws Exception {
        when(placeService.getPlacesByAdminId(adminId)).thenReturn(List.of());

        mockMvc.perform(get("/api/places/admin/my-places"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ==================== GET PLACES BY ADMIN (PATH VARIABLE) ====================

    @Test
    @WithMockUser(username = adminId, roles = {"ADMIN"})
    void getPlacesByAdmin_Success() throws Exception {
        when(placeService.getPlacesByAdminId(adminId)).thenReturn(List.of(testPlace));

        mockMvc.perform(get("/api/places/admin/{adminId}", adminId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(placeId));
    }

    @Test
    @WithMockUser(username = "otherAdmin", roles = {"ADMIN"})
    void getPlacesByAdmin_Forbidden() throws Exception {
        mockMvc.perform(get("/api/places/admin/{adminId}", adminId))
                .andExpect(status().isForbidden());
    }

    // ==================== GET PLACES BY TYPE ====================

    @Test
    void getPlacesByType_Success() throws Exception {
        when(placeService.getPlacesByType("SHOP")).thenReturn(List.of(testPlace));

        mockMvc.perform(get("/api/places/type/SHOP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("SHOP"));
    }

    // ==================== GET NEARBY PLACES ====================

    @Test
    void getNearbyPlaces_Success() throws Exception {
        when(placeService.getNearbyPlaces(10.0, 20.0, 5.0)).thenReturn(List.of(testPlace));

        mockMvc.perform(get("/api/places/nearby")
                        .param("longitude", "10.0")
                        .param("latitude", "20.0")
                        .param("radius", "5.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(placeId));
    }

    // ==================== UPDATE PLACE ====================

    @Test
    @WithMockUser(username = adminId, roles = {"ADMIN"})
    void updatePlace_Success() throws Exception {
        PlaceDTO updateRequest = new PlaceDTO();
        updateRequest.setName("Updated Place");
        updateRequest.setType("RESTAURANT");
        updateRequest.setAddress("456 New St");
        updateRequest.setLocation(new double[]{30.0, 40.0});
        updateRequest.setAdminId(adminId);
        updateRequest.setIsActive(false);

        Place updatedPlace = new Place();
        updatedPlace.setId(placeId);
        updatedPlace.setName("Updated Place");
        updatedPlace.setType("RESTAURANT");
        updatedPlace.setAddress("456 New St");
        updatedPlace.setLocation(new GeoJsonPoint(30.0, 40.0));
        updatedPlace.setAdminId(adminId);
        updatedPlace.setIsActive(false);

        when(placeService.isPlaceOwnedByAdmin(placeId, adminId)).thenReturn(true);
        when(placeService.updatePlace(eq(placeId), any(PlaceDTO.class))).thenReturn(updatedPlace);

        mockMvc.perform(put("/api/places/{id}", placeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Place"))
                .andExpect(jsonPath("$.type").value("RESTAURANT"));
    }

    @Test
    @WithMockUser(username = "otherAdmin", roles = {"ADMIN"})
    void updatePlace_Forbidden() throws Exception {
        PlaceDTO updateRequest = new PlaceDTO();
        updateRequest.setName("Updated Place");
        updateRequest.setType("RESTAURANT");
        updateRequest.setAddress("456 New St");
        updateRequest.setLocation(new double[]{30.0, 40.0});
        updateRequest.setAdminId(adminId);
        updateRequest.setIsActive(false);

        when(placeService.isPlaceOwnedByAdmin(placeId, "otherAdmin")).thenReturn(false);

        mockMvc.perform(put("/api/places/{id}", placeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = adminId, roles = {"ADMIN"})
    void updatePlace_ServiceException() throws Exception {
        PlaceDTO updateRequest = new PlaceDTO();
        updateRequest.setName("Updated Place");
        updateRequest.setType("RESTAURANT");
        updateRequest.setAddress("456 New St");
        updateRequest.setLocation(new double[]{30.0, 40.0});
        updateRequest.setAdminId(adminId);
        updateRequest.setIsActive(false);

        when(placeService.isPlaceOwnedByAdmin(placeId, adminId)).thenReturn(true);
        when(placeService.updatePlace(eq(placeId), any(PlaceDTO.class)))
                .thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(put("/api/places/{id}", placeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isInternalServerError());
    }

    // ==================== DELETE PLACE ====================

    @Test
    @WithMockUser(username = adminId, roles = {"ADMIN"})
    void deletePlace_Success() throws Exception {
        when(placeService.isPlaceOwnedByAdmin(placeId, adminId)).thenReturn(true);

        mockMvc.perform(delete("/api/places/{id}", placeId))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "otherAdmin", roles = {"ADMIN"})
    void deletePlace_Forbidden() throws Exception {
        when(placeService.isPlaceOwnedByAdmin(placeId, "otherAdmin")).thenReturn(false);

        mockMvc.perform(delete("/api/places/{id}", placeId))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = adminId, roles = {"ADMIN"})
    void deletePlace_ServiceException() throws Exception {
        when(placeService.isPlaceOwnedByAdmin(placeId, adminId)).thenReturn(true);
        doThrow(new RuntimeException("DB error")).when(placeService).deletePlace(placeId);

        mockMvc.perform(delete("/api/places/{id}", placeId))
                .andExpect(status().isInternalServerError());
    }

    // ==================== GET ALL PLACES ====================

    @Test
    void getAllPlaces_Success() throws Exception {
        when(placeService.getAllPlaces()).thenReturn(List.of(testPlace));

        mockMvc.perform(get("/api/places"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(placeId));
    }

    // ==================== GET PAGINATED PLACES ====================

    @Test
    void getPlacesPaginated_Success() throws Exception {
        Page<Place> page = new PageImpl<>(List.of(testPlace), PageRequest.of(0, 20), 1);
        when(placeService.getAllPlacesPaginated(any())).thenReturn(page);

        mockMvc.perform(get("/api/places/paginated")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(placeId));
    }

    // ==================== GET TOP RATED PLACES ====================

    @Test
    void getTopRatedPlaces_Success() throws Exception {
        when(placeService.getTopRatedPlaces(3)).thenReturn(List.of(testPlace));

        mockMvc.perform(get("/api/places/top-rated")
                        .param("limit", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(placeId));
    }
}