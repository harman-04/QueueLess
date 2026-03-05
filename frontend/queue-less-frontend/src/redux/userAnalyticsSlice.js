// src/redux/userAnalyticsSlice.js
import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import axiosInstance from '../utils/axiosInstance';

export const fetchUserTokenHistory = createAsyncThunk(
  'userAnalytics/fetchTokenHistory',
  async (days = 30, { rejectWithValue }) => {
    try {
      const response = await axiosInstance.get(`/user/analytics/token-history?days=${days}`);
      return response.data;
    } catch (error) {
      return rejectWithValue(error.response?.data || error.message);
    }
  }
);

const initialState = {
  tokenHistory: { dates: [], counts: [] },
  loading: false,
  error: null,
};

const userAnalyticsSlice = createSlice({
  name: 'userAnalytics',
  initialState,
  reducers: {},
  extraReducers: (builder) => {
    builder
      .addCase(fetchUserTokenHistory.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchUserTokenHistory.fulfilled, (state, action) => {
        state.loading = false;
        state.tokenHistory = action.payload;
      })
      .addCase(fetchUserTokenHistory.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload;
      });
  },
});

export default userAnalyticsSlice.reducer;