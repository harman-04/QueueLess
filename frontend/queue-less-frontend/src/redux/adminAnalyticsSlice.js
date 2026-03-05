// src/redux/adminAnalyticsSlice.js
import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import { adminService } from '../services/adminService';

export const fetchTokensOverTime = createAsyncThunk(
  'adminAnalytics/fetchTokensOverTime',
  async (days = 30, { rejectWithValue }) => {
    try {
      const response = await adminService.getTokensOverTime(days);
      return response.data;
    } catch (error) {
      return rejectWithValue(error.response?.data || error.message);
    }
  }
);

export const fetchBusiestHours = createAsyncThunk(
  'adminAnalytics/fetchBusiestHours',
  async (_, { rejectWithValue }) => {
    try {
      const response = await adminService.getBusiestHours();
      return response.data;
    } catch (error) {
      return rejectWithValue(error.response?.data || error.message);
    }
  }
);

const initialState = {
  tokensOverTime: { dates: [], counts: [] },
  busiestHours: {},
  loading: false,
  error: null,
};

const adminAnalyticsSlice = createSlice({
  name: 'adminAnalytics',
  initialState,
  reducers: {},
  extraReducers: (builder) => {
    builder
      .addCase(fetchTokensOverTime.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchTokensOverTime.fulfilled, (state, action) => {
        state.loading = false;
        state.tokensOverTime = action.payload;
      })
      .addCase(fetchTokensOverTime.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload;
      })
      .addCase(fetchBusiestHours.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchBusiestHours.fulfilled, (state, action) => {
        state.loading = false;
        state.busiestHours = action.payload;
      })
      .addCase(fetchBusiestHours.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload;
      });
  },
});

export default adminAnalyticsSlice.reducer;