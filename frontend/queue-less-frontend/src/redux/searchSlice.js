import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import { searchService } from '../services/searchService';

// Async thunks
export const performSearch = createAsyncThunk(
  'search/performSearch',
  async ({ filters, page, size, sortBy, sortDirection }, { rejectWithValue }) => {
    try {
      return await searchService.comprehensiveSearch(filters, page, size, sortBy, sortDirection);
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
      statistics: null
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
    error: null,
    currentPage: 0,
    totalPages: 0,
    sortBy: 'name',
    sortDirection: 'asc'
  },
  reducers: {
    setFilters: (state, action) => {
      state.filters = { ...state.filters, ...action.payload };
      state.currentPage = 0; // Reset to first page when filters change
    },
    setSorting: (state, action) => {
      state.sortBy = action.payload.sortBy;
      state.sortDirection = action.payload.sortDirection;
    },
    setPage: (state, action) => {
      state.currentPage = action.payload;
    },
    clearResults: (state) => {
      state.results = {
        places: [],
        services: [],
        queues: [],
        totalPlaces: 0,
        totalServices: 0,
        totalQueues: 0,
        statistics: null
      };
      state.currentPage = 0;
      state.totalPages = 0;
    },
    clearError: (state) => {
      state.error = null;
    }
  },
  extraReducers: (builder) => {
    builder
      // Perform Search
      .addCase(performSearch.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(performSearch.fulfilled, (state, action) => {
        state.loading = false;
        state.results = action.payload;
        state.totalPages = Math.ceil(
          Math.max(
            action.payload.totalPlaces || 0,
            action.payload.totalServices || 0,
            action.payload.totalQueues || 0
          ) / 20
        );
      })
      .addCase(performSearch.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload;
      })
      // Nearby Search
      .addCase(performNearbySearch.fulfilled, (state, action) => {
        state.results.places = action.payload;
        state.results.totalPlaces = action.payload.length;
      })
      // Fetch Filter Options
      .addCase(fetchFilterOptions.fulfilled, (state, action) => {
        state.filterOptions = action.payload;
      });
  }
});

export const { setFilters, setSorting, setPage, clearResults, clearError } = searchSlice.actions;
export default searchSlice.reducer;