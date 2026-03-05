import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import axiosInstance from '../utils/axiosInstance';

export const fetchProviderTokensOverTime = createAsyncThunk(
  'providerAnalytics/fetchTokensOverTime',
  async (days = 30, { rejectWithValue }) => {
    try {
      const response = await axiosInstance.get(`/providers/analytics/tokens-over-time?days=${days}`);
      return response.data;
    } catch (error) {
      return rejectWithValue(error.response?.data || error.message);
    }
  }
);

export const fetchProviderBusiestHours = createAsyncThunk(
  'providerAnalytics/fetchBusiestHours',
  async (_, { rejectWithValue }) => {
    try {
      const response = await axiosInstance.get('/providers/analytics/busiest-hours');
      return response.data;
    } catch (error) {
      return rejectWithValue(error.response?.data || error.message);
    }
  }
);

export const fetchProviderAverageWaitTime = createAsyncThunk(
  'providerAnalytics/fetchAverageWaitTime',
  async (days = 30, { rejectWithValue }) => {
    try {
      const response = await axiosInstance.get(`/providers/analytics/average-wait-time?days=${days}`);
      return response.data;
    } catch (error) {
      return rejectWithValue(error.response?.data || error.message);
    }
  }
);

const initialState = {
  tokensOverTime: { dates: [], counts: [] },
  busiestHours: {},
  averageWaitTime: { dates: [], averages: [] },
  loading: false,
  error: null,
};

const providerAnalyticsSlice = createSlice({
  name: 'providerAnalytics',
  initialState,
  reducers: {},
  extraReducers: (builder) => {
    builder
      .addCase(fetchProviderTokensOverTime.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchProviderTokensOverTime.fulfilled, (state, action) => {
        state.loading = false;
        state.tokensOverTime = action.payload;
      })
      .addCase(fetchProviderTokensOverTime.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload;
      })
      .addCase(fetchProviderBusiestHours.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchProviderBusiestHours.fulfilled, (state, action) => {
        state.loading = false;
        state.busiestHours = action.payload;
      })
      .addCase(fetchProviderBusiestHours.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload;
      })
      .addCase(fetchProviderAverageWaitTime.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchProviderAverageWaitTime.fulfilled, (state, action) => {
        state.loading = false;
        state.averageWaitTime = action.payload;
      })
      .addCase(fetchProviderAverageWaitTime.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload;
      });
  },
});

export default providerAnalyticsSlice.reducer;