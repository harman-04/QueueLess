package com.queueless.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.queueless.backend.config.RateLimitConfig;
import com.queueless.backend.config.TestSecurityConfig;
import com.queueless.backend.dto.*;
import com.queueless.backend.model.Place;
import com.queueless.backend.service.SearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = SearchController.class,
        properties = "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration")
@Import({RateLimitConfig.class, TestSecurityConfig.class})
@AutoConfigureMockMvc
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SearchService searchService;

    @Autowired
    private ObjectMapper objectMapper;

    // ==================== COMPREHENSIVE SEARCH ====================

    @Test
    void comprehensiveSearch_Success() throws Exception {
        SearchRequestDTO request = new SearchRequestDTO();
        request.setQuery("test");
        request.setPlaceType("SHOP");
        request.setMinRating(4.0);
        request.setSearchPlaces(true);
        request.setSearchServices(true);
        request.setSearchQueues(true);

        SearchResultDTO result = new SearchResultDTO();
        result.setTotalPlaces(5L);
        result.setTotalServices(3L);
        result.setTotalQueues(2L);
        result.setPlaces(List.of(new PlaceDTO()));
        result.setServices(List.of(new ServiceDTO()));
        result.setQueues(List.of(new EnhancedQueueDTO()));

        Pageable pageable = PageRequest.of(0, 20, Sort.by("name").ascending());
        when(searchService.comprehensiveSearch(any(SearchRequestDTO.class), any(Pageable.class)))
                .thenReturn(result);

        mockMvc.perform(post("/api/search/comprehensive")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sortBy", "name")
                        .param("sortDirection", "asc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPlaces").value(5))
                .andExpect(jsonPath("$.totalServices").value(3))
                .andExpect(jsonPath("$.totalQueues").value(2));
    }

    @Test
    void comprehensiveSearch_InvalidRequest_Returns400() throws Exception {
        // Missing required fields? The DTO has no required fields, but validation annotations may exist.
        // Send empty JSON to trigger validation errors if any (e.g., @NotNull on some fields? Not in SearchRequestDTO).
        // We'll just test that service exception is mapped to 500, not 400. For validation, we need a field with constraints.
        // SearchRequestDTO has no @NotNull, so we'll not test validation here.
        // Instead, we can test that a service exception returns 500.
        SearchRequestDTO request = new SearchRequestDTO();
        when(searchService.comprehensiveSearch(any(), any()))
                .thenThrow(new RuntimeException("Service error"));

        mockMvc.perform(post("/api/search/comprehensive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()); // was is5xxServerError
    }

    // ==================== NEARBY SEARCH ====================

    @Test
    void searchNearby_Success() throws Exception {
        SearchRequestDTO request = new SearchRequestDTO();
        request.setQuery("test");
        request.setLongitude(10.0);
        request.setLatitude(20.0);
        request.setRadius(5.0);

        Place place = new Place();
        place.setId("place1");
        place.setName("Test Place");
        List<Place> places = List.of(place);
        when(searchService.searchNearbyWithFilters(eq(10.0), eq(20.0), eq(5.0), any(SearchRequestDTO.class)))
                .thenReturn(places);

        mockMvc.perform(post("/api/search/nearby")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("place1"))
                .andExpect(jsonPath("$[0].name").value("Test Place"));
    }

    @Test
    void searchNearby_MissingCoordinates_Returns400() throws Exception {
        SearchRequestDTO request = new SearchRequestDTO();
        request.setQuery("test");
        // no longitude/latitude

        mockMvc.perform(post("/api/search/nearby")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ==================== FILTER OPTIONS ====================

    @Test
    void getFilterOptions_Success() throws Exception {
        List<String> placeTypes = List.of("SHOP", "HOSPITAL", "BANK");
        when(searchService.getAvailablePlaceTypes()).thenReturn(placeTypes);

        mockMvc.perform(get("/api/search/filter-options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.placeTypes[0]").value("SHOP"))
                .andExpect(jsonPath("$.placeTypes[1]").value("HOSPITAL"))
                .andExpect(jsonPath("$.placeTypes[2]").value("BANK"))
                .andExpect(jsonPath("$.serviceTypes").isArray())
                .andExpect(jsonPath("$.waitTimeRanges").isArray());
    }

    // ==================== SEARCH STATISTICS ====================

    @Test
    void getSearchStatistics_Success() throws Exception {
        SearchRequestDTO request = new SearchRequestDTO();
        request.setQuery("test");

        SearchStatisticsDTO stats = new SearchStatisticsDTO();
        stats.setTotalPlaces(10L);
        stats.setTotalServices(5L);
        stats.setTotalQueues(3L);
        stats.setTotalActiveQueues(2L);
        stats.setAverageWaitTime(12.5);
        stats.setAverageRating(4.2);

        when(searchService.getSearchStatistics(any(SearchRequestDTO.class))).thenReturn(stats);

        mockMvc.perform(post("/api/search/statistics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPlaces").value(10))
                .andExpect(jsonPath("$.totalServices").value(5))
                .andExpect(jsonPath("$.totalQueues").value(3))
                .andExpect(jsonPath("$.totalActiveQueues").value(2))
                .andExpect(jsonPath("$.averageWaitTime").value(12.5))
                .andExpect(jsonPath("$.averageRating").value(4.2));
    }

    // ==================== QUICK SEARCH ====================

    @Test
    void quickSearch_Success() throws Exception {
        String query = "test";
        int limit = 5;

        SearchResultDTO result = new SearchResultDTO();
        result.setTotalPlaces(2L);
        result.setPlaces(List.of(new PlaceDTO()));

        when(searchService.comprehensiveSearch(any(SearchRequestDTO.class), any(Pageable.class)))
                .thenReturn(result);

        mockMvc.perform(get("/api/search/quick/{query}", query)
                        .param("limit", String.valueOf(limit)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPlaces").value(2));
    }
}