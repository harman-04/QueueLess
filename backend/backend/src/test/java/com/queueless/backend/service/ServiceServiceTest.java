package com.queueless.backend.service;

import com.queueless.backend.dto.ServiceDTO;
import com.queueless.backend.model.Service;
import com.queueless.backend.repository.ServiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceServiceTest {

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private PlaceService placeService;

    @InjectMocks
    private ServiceService serviceService;

    private Service testService;
    private final String serviceId = "service123";
    private final String placeId = "place123";
    private final String adminId = "admin123";

    @BeforeEach
    void setUp() {
        testService = Service.builder()
                .id(serviceId)
                .placeId(placeId)
                .name("Test Service")
                .description("A test service")
                .averageServiceTime(15)
                .supportsGroupToken(true)
                .emergencySupport(false)
                .isActive(true)
                .build();
    }

    // ================= CREATE SERVICE =================

    @Test
    void createServiceSuccess() {
        ServiceDTO dto = ServiceDTO.fromEntity(testService);
        when(serviceRepository.save(any(Service.class))).thenAnswer(inv -> inv.getArgument(0));

        Service created = serviceService.createService(dto);

        assertNotNull(created);
        assertEquals(testService.getName(), created.getName());
        assertEquals(testService.getPlaceId(), created.getPlaceId());
        verify(serviceRepository).save(any(Service.class));
    }

    // ================= IS SERVICE OWNED BY ADMIN =================

    @Test
    void isServiceOwnedByAdminTrue() {
        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(testService));
        when(placeService.isPlaceOwnedByAdmin(placeId, adminId)).thenReturn(true);

        boolean result = serviceService.isServiceOwnedByAdmin(serviceId, adminId);

        assertTrue(result);
        verify(serviceRepository).findById(serviceId);
        verify(placeService).isPlaceOwnedByAdmin(placeId, adminId);
    }

    @Test
    void isServiceOwnedByAdminFalse() {
        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(testService));
        when(placeService.isPlaceOwnedByAdmin(placeId, adminId)).thenReturn(false);

        boolean result = serviceService.isServiceOwnedByAdmin(serviceId, adminId);

        assertFalse(result);
    }

    @Test
    void isServiceOwnedByAdminServiceNotFound() {
        when(serviceRepository.findById(serviceId)).thenReturn(Optional.empty());

        boolean result = serviceService.isServiceOwnedByAdmin(serviceId, adminId);

        assertFalse(result);
        verifyNoInteractions(placeService);
    }

    // ================= GET SERVICE BY ID =================

    @Test
    void getServiceByIdSuccess() {
        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(testService));

        Service found = serviceService.getServiceById(serviceId);

        assertNotNull(found);
        assertEquals(serviceId, found.getId());
    }

    @Test
    void getServiceByIdNotFound() {
        when(serviceRepository.findById(serviceId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> serviceService.getServiceById(serviceId));
        assertEquals("Service not found with id: " + serviceId, exception.getMessage());
    }

    // ================= GET SERVICES BY PLACE ID =================

    @Test
    void getServicesByPlaceId() {
        List<Service> services = List.of(testService);
        when(serviceRepository.findByPlaceId(placeId)).thenReturn(services);

        List<Service> result = serviceService.getServicesByPlaceId(placeId);

        assertEquals(1, result.size());
        assertEquals(serviceId, result.get(0).getId());
    }

    // ================= UPDATE SERVICE =================

    @Test
    void updateServiceSuccess() {
        ServiceDTO updateDto = new ServiceDTO();
        updateDto.setName("Updated Name");
        updateDto.setDescription("Updated description");
        updateDto.setAverageServiceTime(20);
        updateDto.setSupportsGroupToken(false);
        updateDto.setEmergencySupport(true);
        updateDto.setIsActive(false);

        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(testService));
        when(serviceRepository.save(any(Service.class))).thenAnswer(inv -> inv.getArgument(0));

        Service updated = serviceService.updateService(serviceId, updateDto);

        assertEquals("Updated Name", updated.getName());
        assertEquals("Updated description", updated.getDescription());
        assertEquals(20, updated.getAverageServiceTime());
        assertFalse(updated.getSupportsGroupToken());
        assertTrue(updated.getEmergencySupport());
        assertFalse(updated.getIsActive());
        // unchanged fields
        assertEquals(placeId, updated.getPlaceId());
    }

    @Test
    void updateServiceNotFound() {
        when(serviceRepository.findById(serviceId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> serviceService.updateService(serviceId, new ServiceDTO()));
    }

    // ================= DELETE SERVICE =================

    @Test
    void deleteServiceSuccess() {
        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(testService));
        doNothing().when(serviceRepository).delete(testService);

        assertDoesNotThrow(() -> serviceService.deleteService(serviceId));

        verify(serviceRepository).delete(testService);
    }

    @Test
    void deleteServiceNotFound() {
        when(serviceRepository.findById(serviceId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> serviceService.deleteService(serviceId));
        verify(serviceRepository, never()).delete(any());
    }

    // ================= GET ALL SERVICES =================

    @Test
    void getAllServices() {
        List<Service> services = List.of(testService);
        when(serviceRepository.findAll()).thenReturn(services);

        List<Service> result = serviceService.getAllServices();

        assertEquals(1, result.size());
        assertEquals(serviceId, result.get(0).getId());
    }
}