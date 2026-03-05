import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import { searchService } from '../services/searchService';

// Async thunks
export const performSearch = createAsyncThunk(
  'search/performSearch',
  async ({ filters, page = 0, size = 20, sortBy, sortDirection, type = 'all' }, { rejectWithValue, getState }) => {
    try {
      const response = await searchService.comprehensiveSearch(filters, page, size, sortBy, sortDirection);
      return { ...response, type };
    } catch (error) {
      return rejectWithValue(error.response?.data || error.message);
    }
  }
);


export const performNearbySearch = createAsyncThunk(
  'search/performNearbySearch',
  async (filters, { rejectWithValue }) => {
    try {
      return await searchService.searchNearby(filters);
    } catch (error) {
      return rejectWithValue(error.response?.data || error.message);
    }
  }
);

export const fetchFilterOptions = createAsyncThunk(
  'search/fetchFilterOptions',
  async (_, { rejectWithValue }) => {
    try {
      return await searchService.getFilterOptions();
    } catch (error) {
      return rejectWithValue(error.response?.data || error.message);
    }
  }
);

const searchSlice = createSlice({
  name: 'search',
  initialState: {
    results: {
      places: [],
      services: [],
      queues: [],
      totalPlaces: 0,
      totalServices: 0,
      totalQueues: 0,
      statistics: null,
      placesPage: 0,
      placesTotalPages: 0,
      servicesPage: 0,
      servicesTotalPages: 0,
      queuesPage: 0,
      queuesTotalPages: 0
    },
    filters: {
      query: '',
      placeType: '',
      placeTypes: [],
      minRating: 0,
      maxWaitTime: null,
      supportsGroupToken: null,
      emergencySupport: null,
      isActive: true,
      longitude: null,
      latitude: null,
      radius: 5,
      searchPlaces: true,
      searchServices: true,
      searchQueues: true
    },
    filterOptions: {
      placeTypes: [],
      serviceTypes: [],
      waitTimeRanges: []
    },
    loading: false,
    loadingMore: {
      places: false,
      services: false,
      queues: false
    },
    error: null,
    sortBy: 'name',
    sortDirection: 'asc'
  },
  reducers: {
    setFilters: (state, action) => {
      state.filters = { ...state.filters, ...action.payload };
      // Reset all pagination when filters change
      state.results.places = [];
      state.results.services = [];
      state.results.queues = [];
      state.results.placesPage = 0;
      state.results.servicesPage = 0;
      state.results.queuesPage = 0;
    },
    setSorting: (state, action) => {
      state.sortBy = action.payload.sortBy;
      state.sortDirection = action.payload.sortDirection;
      // Reset results when sorting changes (new search)
      state.results.places = [];
      state.results.services = [];
      state.results.queues = [];
      state.results.placesPage = 0;
      state.results.servicesPage = 0;
      state.results.queuesPage = 0;
    },
    clearResults: (state) => {
      state.results = {
        places: [],
        services: [],
        queues: [],
        totalPlaces: 0,
        totalServices: 0,
        totalQueues: 0,
        statistics: null,
        placesPage: 0,
        placesTotalPages: 0,
        servicesPage: 0,
        servicesTotalPages: 0,
        queuesPage: 0,
        queuesTotalPages: 0
      };
    },
    clearError: (state) => {
      state.error = null;
    }
  },
  extraReducers: (builder) => {
    builder
      // Perform Search
      .addCase(performSearch.pending, (state, action) => {
        const { type } = action.meta.arg;
        if (type === 'all') {
          state.loading = true;
          state.loadingMore = { places: false, services: false, queues: false };
        } else {
          state.loadingMore[type] = true;
        }
        state.error = null;
      })
      .addCase(performSearch.fulfilled, (state, action) => {
        const { type, places, services, queues, totalPlaces, totalServices, totalQueues,
          placesPage, placesTotalPages, servicesPage, servicesTotalPages,
          queuesPage, queuesTotalPages, statistics } = action.payload;

        if (type === 'all') {
          // Replace all results
          state.results.places = places || [];
          state.results.services = services || [];
          state.results.queues = queues || [];
          state.results.totalPlaces = totalPlaces || 0;
          state.results.totalServices = totalServices || 0;
          state.results.totalQueues = totalQueues || 0;
          state.results.placesPage = placesPage || 0;
          state.results.placesTotalPages = placesTotalPages || 0;
          state.results.servicesPage = servicesPage || 0;
          state.results.servicesTotalPages = servicesTotalPages || 0;
          state.results.queuesPage = queuesPage || 0;
          state.results.queuesTotalPages = queuesTotalPages || 0;
          state.results.statistics = statistics || null;
          state.loading = false;
        } else {
          // Append to specific type
          if (type === 'places') {
            state.results.places = [...state.results.places, ...(places || [])];
            state.results.totalPlaces = totalPlaces || 0;
            state.results.placesPage = placesPage || 0;
            state.results.placesTotalPages = placesTotalPages || 0;
          } else if (type === 'services') {
            state.results.services = [...state.results.services, ...(services || [])];
            state.results.totalServices = totalServices || 0;
            state.results.servicesPage = servicesPage || 0;
            state.results.servicesTotalPages = servicesTotalPages || 0;
          } else if (type === 'queues') {
            state.results.queues = [...state.results.queues, ...(queues || [])];
            state.results.totalQueues = totalQueues || 0;
            state.results.queuesPage = queuesPage || 0;
            state.results.queuesTotalPages = queuesTotalPages || 0;
          }
          state.loadingMore[type] = false;
        }
      })
      .addCase(performSearch.rejected, (state, action) => {
        const { type } = action.meta.arg;
        if (type === 'all') {
          state.loading = false;
        } else {
          state.loadingMore[type] = false;
        }
        state.error = action.payload;
      })
      // Nearby Search
      .addCase(performNearbySearch.fulfilled, (state, action) => {
        state.results.places = action.payload;
        state.results.totalPlaces = action.payload.length;
        // Reset pagination
        state.results.placesPage = 0;
        state.results.placesTotalPages = 1;
      })
      // Fetch Filter Options
      .addCase(fetchFilterOptions.fulfilled, (state, action) => {
        state.filterOptions = action.payload;
      });
  }
});

export const { setFilters, setSorting, clearResults, clearError } = searchSlice.actions;
export default searchSlice.reducer;
