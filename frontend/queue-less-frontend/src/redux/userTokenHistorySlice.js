import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import axiosInstance from '../utils/axiosInstance';

export const fetchUserTokenHistory = createAsyncThunk(
  'userTokenHistory/fetch',
  async ({ days = 30, page = 0, size = 20 }, { rejectWithValue, getState }) => {
    try {
      // Request one extra to detect next page
      const response = await axiosInstance.get('/user/tokens', {
        params: { days, page, size: size + 1 }
      });
      const data = response.data;
      // Return both the data and the original size
      return { data, originalSize: size };
    } catch (error) {
      return rejectWithValue(error.response?.data?.message || 'Failed to fetch token history');
    }
  }
);

const userTokenHistorySlice = createSlice({
  name: 'userTokenHistory',
  initialState: {
    tokens: [],
    loading: false,
    loadingMore: false,
    error: null,
    hasMore: true,
    page: 0,
    size: 20
  },
  reducers: {
    resetHistory: (state) => {
      state.tokens = [];
      state.page = 0;
      state.hasMore = true;
      state.loading = false;
      state.loadingMore = false;
      state.error = null;
    }
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchUserTokenHistory.pending, (state, action) => {
        const { page } = action.meta.arg;
        if (page === 0) {
          state.loading = true;
          state.loadingMore = false;
        } else {
          state.loadingMore = true;
        }
        state.error = null;
      })
      .addCase(fetchUserTokenHistory.fulfilled, (state, action) => {
        const { data, originalSize } = action.payload;
        const { page } = action.meta.arg;

        let newTokens = [];
        let hasMore = false;

        if (data.length > originalSize) {
          // There is a next page
          newTokens = data.slice(0, originalSize);
          hasMore = true;
        } else {
          // No more pages
          newTokens = data;
          hasMore = false;
        }

        if (page === 0) {
          state.tokens = newTokens;
        } else {
          // Avoid duplicates (though they shouldn't occur if backend pagination is correct)
          const existingIds = new Set(state.tokens.map(t => t.tokenId));
          const uniqueNew = newTokens.filter(t => !existingIds.has(t.tokenId));
          state.tokens = [...state.tokens, ...uniqueNew];
        }

        state.page = page + 1; // next page to fetch
        state.hasMore = hasMore;
        state.loading = false;
        state.loadingMore = false;
      })
      .addCase(fetchUserTokenHistory.rejected, (state, action) => {
        state.loading = false;
        state.loadingMore = false;
        state.error = action.payload;
      });
  }
});

export const { resetHistory } = userTokenHistorySlice.actions;
export default userTokenHistorySlice.reducer;