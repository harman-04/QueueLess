package com.queueless.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.queueless.backend.dto.ServiceDTO;
import com.queueless.backend.model.Service;
import com.queueless.backend.service.PlaceService;
import com.queueless.backend.service.ServiceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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

@SpringBootTest(properties = {
        "spring.cache.type=none",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration"
})
@AutoConfigureMockMvc
class ServiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ServiceService serviceService;

    @MockitoBean
    private PlaceService placeService;

    @Autowired
    private ObjectMapper objectMapper;

    private final String adminId = "admin123";
    private final String placeId = "place123";
    private final String serviceId = "service123";

    private Service createTestService() {
        return Service.builder()
                .id(serviceId)
                .placeId(placeId)
                .name("Test Service")
                .description("Description")
                .averageServiceTime(10)
                .supportsGroupToken(true)
                .emergencySupport(false)
                .isActive(true)
                .build();
    }

    private ServiceDTO createTestServiceDTO() {
        ServiceDTO dto = new ServiceDTO();
        dto.setPlaceId(placeId);
        dto.setName("Test Service");
        dto.setDescription("Description");
        dto.setAverageServiceTime(10);
        dto.setSupportsGroupToken(true);
        dto.setEmergencySupport(false);
        dto.setIsActive(true);
        return dto;
    }

    // ==================== CREATE SERVICE ====================

    @Test
    @WithMockUser(username = adminId, roles = {"ADMIN"})
    void createService_Success() throws Exception {
        ServiceDTO request = createTestServiceDTO();
        Service service = createTestService();

        when(placeService.isPlaceOwnedByAdmin(placeId, adminId)).thenReturn(true);
        when(serviceService.createService(any(ServiceDTO.class))).thenReturn(service);

        mockMvc.perform(post("/api/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(serviceId))
                .andExpect(jsonPath("$.name").value("Test Service"));
    }

    @Test
    @WithMockUser(username = adminId, roles = {"ADMIN"})
    void createService_PlaceNotOwned() throws Exception {
        ServiceDTO request = createTestServiceDTO();

        when(placeService.isPlaceOwnedByAdmin(placeId, adminId)).thenReturn(false);

        mockMvc.perform(post("/api/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = adminId, roles = {"ADMIN"})
    void createService_ServiceException() throws Exception {
        ServiceDTO request = createTestServiceDTO();

        when(placeService.isPlaceOwnedByAdmin(placeId, adminId)).thenReturn(true);
        when(serviceService.createService(any(ServiceDTO.class))).thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(post("/api/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }

    // ==================== GET SERVICE BY ID ====================

    @Test
    void getService_Success() throws Exception {
        Service service = createTestService();
        when(serviceService.getServiceById(serviceId)).thenReturn(service);

        mockMvc.perform(get("/api/services/{id}", serviceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(serviceId))
                .andExpect(jsonPath("$.name").value("Test Service"));
    }

    @Test
    void getService_NotFound() throws Exception {
        when(serviceService.getServiceById(serviceId)).thenThrow(new RuntimeException("Not found"));

        mockMvc.perform(get("/api/services/{id}", serviceId))
                .andExpect(status().isNotFound());
    }

    // ==================== GET SERVICES BY PLACE ====================

    @Test
    void getServicesByPlace_Success() throws Exception {
        Service service = createTestService();
        when(serviceService.getServicesByPlaceId(placeId)).thenReturn(List.of(service));

        mockMvc.perform(get("/api/services/place/{placeId}", placeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(serviceId))
                .andExpect(jsonPath("$[0].name").value("Test Service"));
    }

    @Test
    void getServicesByPlace_Empty() throws Exception {
        when(serviceService.getServicesByPlaceId(placeId)).thenReturn(List.of());

        mockMvc.perform(get("/api/services/place/{placeId}", placeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ==================== UPDATE SERVICE ====================

    @Test
    @WithMockUser(username = adminId, roles = {"ADMIN"})
    void updateService_Success() throws Exception {
        ServiceDTO request = createTestServiceDTO();
        request.setName("Updated Name");

        Service updatedService = createTestService();
        updatedService.setName("Updated Name");

        when(serviceService.isServiceOwnedByAdmin(serviceId, adminId)).thenReturn(true);
        when(serviceService.updateService(eq(serviceId), any(ServiceDTO.class))).thenReturn(updatedService);

        mockMvc.perform(put("/api/services/{id}", serviceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"));
    }

    @Test
    @WithMockUser(username = adminId, roles = {"ADMIN"})
    void updateService_Forbidden() throws Exception {
        ServiceDTO request = createTestServiceDTO();

        when(serviceService.isServiceOwnedByAdmin(serviceId, adminId)).thenReturn(false);

        mockMvc.perform(put("/api/services/{id}", serviceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = adminId, roles = {"ADMIN"})
    void updateService_NotFound() throws Exception {
        ServiceDTO request = createTestServiceDTO();

        when(serviceService.isServiceOwnedByAdmin(serviceId, adminId)).thenReturn(true);
        when(serviceService.updateService(eq(serviceId), any(ServiceDTO.class)))
                .thenThrow(new RuntimeException("Not found"));

        mockMvc.perform(put("/api/services/{id}", serviceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError()); // controller returns 500 for any exception
    }

    // ==================== DELETE SERVICE ====================

    @Test
    @WithMockUser(username = adminId, roles = {"ADMIN"})
    void deleteService_Success() throws Exception {
        when(serviceService.isServiceOwnedByAdmin(serviceId, adminId)).thenReturn(true);

        mockMvc.perform(delete("/api/services/{id}", serviceId))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = adminId, roles = {"ADMIN"})
    void deleteService_Forbidden() throws Exception {
        when(serviceService.isServiceOwnedByAdmin(serviceId, adminId)).thenReturn(false);

        mockMvc.perform(delete("/api/services/{id}", serviceId))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = adminId, roles = {"ADMIN"})
    void deleteService_NotFound() throws Exception {
        when(serviceService.isServiceOwnedByAdmin(serviceId, adminId)).thenReturn(true);
        doThrow(new RuntimeException("Not found")).when(serviceService).deleteService(serviceId);

        mockMvc.perform(delete("/api/services/{id}", serviceId))
                .andExpect(status().isInternalServerError());
    }

    // ==================== GET ALL SERVICES ====================

    @Test
    void getAllServices_Success() throws Exception {
        Service service = createTestService();
        when(serviceService.getAllServices()).thenReturn(List.of(service));

        mockMvc.perform(get("/api/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(serviceId))
                .andExpect(jsonPath("$[0].name").value("Test Service"));
    }
}