package com.queueless.backend.service;

import com.queueless.backend.dto.*;
import com.queueless.backend.model.Place;
import com.queueless.backend.model.Queue;
import com.queueless.backend.model.Service;
import com.queueless.backend.repository.PlaceRepository;
import com.queueless.backend.repository.QueueRepository;
import com.queueless.backend.repository.ServiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private QueueRepository queueRepository;

    @Mock
    private PlaceService placeService;

    @Mock
    private QueueService queueService;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private SearchService searchService;

    private Place testPlace;
    private Service testService;
    private Queue testQueue;
    private final String placeId = "place123";
    private final String serviceId = "service123";
    private final String queueId = "queue123";

    @BeforeEach
    void setUp() {
        testPlace = new Place();
        testPlace.setId(placeId);
        testPlace.setName("Test Place");
        testPlace.setType("SHOP");
        testPlace.setAddress("123 Main St");
        testPlace.setDescription("A test place");
        testPlace.setRating(4.5);
        testPlace.setIsActive(true);

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

        testQueue = new Queue("provider123", "Test Queue", placeId, serviceId);
        testQueue.setId(queueId);
        testQueue.setIsActive(true);
        testQueue.setEstimatedWaitTime(10);
    }

    // ================= COMPREHENSIVE SEARCH =================

    @Test
    void comprehensiveSearchPlacesOnly() {
        SearchRequestDTO request = new SearchRequestDTO();
        request.setSearchPlaces(true);
        request.setSearchServices(false);
        request.setSearchQueues(false);
        request.setQuery("test");
        request.setPlaceType("SHOP");
        request.setMinRating(4.0);
        request.setIsActive(true);

        Pageable pageable = PageRequest.of(0, 20, Sort.by("name").ascending());

        List<Place> places = List.of(testPlace);
        long totalPlaces = 1;

        when(mongoTemplate.find(any(Query.class), eq(Place.class))).thenReturn(places);
        when(mongoTemplate.count(any(Query.class), eq(Place.class))).thenReturn(totalPlaces);

        SearchResultDTO result = searchService.comprehensiveSearch(request, pageable);

        assertNotNull(result);
        assertEquals(1, result.getPlaces().size());
        assertEquals(placeId, result.getPlaces().get(0).getId());
        assertEquals(totalPlaces, result.getTotalPlaces());
        assertNull(result.getServices());
        assertNull(result.getQueues());
    }

    @Test
    void comprehensiveSearchServicesOnlyWithPlaceIds() {
        SearchRequestDTO request = new SearchRequestDTO();
        request.setSearchPlaces(true); // to get placeIds
        request.setSearchServices(true);
        request.setSearchQueues(false);
        request.setQuery("test");

        Pageable pageable = PageRequest.of(0, 20, Sort.by("name").ascending());

        List<Place> places = List.of(testPlace);
        when(mongoTemplate.find(any(Query.class), eq(Place.class))).thenReturn(places);
        when(mongoTemplate.count(any(Query.class), eq(Place.class))).thenReturn(1L);

        List<Service> services = List.of(testService);
        when(mongoTemplate.find(any(Query.class), eq(Service.class))).thenReturn(services);
        when(mongoTemplate.count(any(Query.class), eq(Service.class))).thenReturn(1L);

        SearchResultDTO result = searchService.comprehensiveSearch(request, pageable);

        assertEquals(1, result.getServices().size());
        assertEquals(serviceId, result.getServices().get(0).getId());
        assertEquals(1, result.getTotalServices());
    }

    @Test
    void comprehensiveSearchQueuesOnlyWithPlaceIds() {
        SearchRequestDTO request = new SearchRequestDTO();
        request.setSearchPlaces(true);
        request.setSearchServices(false);
        request.setSearchQueues(true);
        request.setQuery("test");
        request.setMaxWaitTime(15);
        request.setSupportsGroupToken(true);
        request.setEmergencySupport(false);
        request.setIsActive(true);

        Pageable pageable = PageRequest.of(0, 20, Sort.by("name").ascending());

        List<Place> places = List.of(testPlace);
        when(mongoTemplate.find(any(Query.class), eq(Place.class))).thenReturn(places);
        when(mongoTemplate.count(any(Query.class), eq(Place.class))).thenReturn(1L);

        List<Queue> queues = List.of(testQueue);
        when(mongoTemplate.find(any(Query.class), eq(Queue.class))).thenReturn(queues);
        when(mongoTemplate.count(any(Query.class), eq(Queue.class))).thenReturn(1L);

        when(placeService.getPlacesByIds(List.of(placeId))).thenReturn(List.of(testPlace));
        when(queueService.calculateCurrentWaitTime(queueId)).thenReturn(8);

        SearchResultDTO result = searchService.comprehensiveSearch(request, pageable);

        assertEquals(1, result.getQueues().size());
        EnhancedQueueDTO dto = result.getQueues().get(0);
        assertEquals(queueId, dto.getId());
        assertEquals(testQueue.getServiceName(), dto.getServiceName());
        assertEquals(testPlace.getName(), dto.getPlaceName());
        assertEquals(testPlace.getAddress(), dto.getPlaceAddress());
        assertEquals(testPlace.getRating(), dto.getPlaceRating());
        assertEquals(8, dto.getCurrentWaitTime());
    }

    // ================= SEARCH NEARBY WITH FILTERS =================

    @Test
    void searchNearbyWithFilters() {
        SearchRequestDTO request = new SearchRequestDTO();
        request.setQuery("test");
        request.setPlaceTypes(List.of("SHOP"));
        request.setMinRating(4.0);
        request.setLongitude(10.0);
        request.setLatitude(20.0);
        request.setRadius(5.0);

        List<Place> places = List.of(testPlace);
        when(placeRepository.searchNearbyWithFilters(
                eq("test"), eq(List.of("SHOP")), eq(4.0), eq(10.0), eq(20.0), eq(5000.0)))
                .thenReturn(places);

        List<Place> result = searchService.searchNearbyWithFilters(10.0, 20.0, 5.0, request);

        assertEquals(1, result.size());
        assertEquals(placeId, result.get(0).getId());
    }

    // ================= GET AVAILABLE PLACE TYPES =================

    @Test
    void getAvailablePlaceTypes() {
        Place p1 = new Place(); p1.setType("SHOP");
        Place p2 = new Place(); p2.setType("HOSPITAL");
        Place p3 = new Place(); p3.setType("SHOP"); // duplicate
        List<Place> places = List.of(p1, p2, p3);

        when(placeRepository.findDistinctTypes()).thenReturn(places);

        List<String> types = searchService.getAvailablePlaceTypes();

        assertEquals(2, types.size());
        assertTrue(types.contains("SHOP"));
        assertTrue(types.contains("HOSPITAL"));
    }

    // ================= GET SEARCH STATISTICS =================

    @Test
    void getSearchStatistics() {
        SearchRequestDTO request = new SearchRequestDTO();
        request.setQuery("test");
        request.setPlaceType("SHOP");
        request.setMinRating(4.0);
        request.setIsActive(true);

        when(placeRepository.countByFilters("test", "SHOP", 4.0, true)).thenReturn(5L);

        SearchStatisticsDTO stats = searchService.getSearchStatistics(request);

        assertNotNull(stats);
        assertEquals(5L, stats.getTotalPlaces());
        // other fields remain null (not set in this method)
    }
}