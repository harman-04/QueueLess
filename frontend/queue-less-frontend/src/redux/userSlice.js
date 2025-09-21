// src/redux/userSlice.js
import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import { userService } from '../services/userService';
import { toast } from 'react-toastify';

// Async thunks
export const fetchFavoritePlaces = createAsyncThunk(
  'user/fetchFavoritePlaces',
  async (_, { rejectWithValue }) => {
    try {
      const response = await userService.getFavoritePlaces();
      return response.data;
    } catch (error) {
      return rejectWithValue(error.response?.data?.message || 'Failed to fetch favorite places');
    }
  }
);

export const addFavoritePlace = createAsyncThunk(
  'user/addFavoritePlace',
  async (placeId, { rejectWithValue }) => {
    try {
      await userService.addFavoritePlace(placeId);
      toast.success('Added to favorites!');
      return placeId;
    } catch (error) {
      return rejectWithValue(error.response?.data?.message || 'Failed to add favorite place');
    }
  }
);

export const removeFavoritePlace = createAsyncThunk(
  'user/removeFavoritePlace',
  async (placeId, { rejectWithValue }) => {
    try {
      await userService.removeFavoritePlace(placeId);
      toast.success('Removed from favorites!');
      return placeId;
    } catch (error) {
      return rejectWithValue(error.response?.data?.message || 'Failed to remove favorite place');
    }
  }
);

export const fetchFavoritePlacesWithDetails = createAsyncThunk(
  'user/fetchFavoritePlacesWithDetails',
  async (_, { rejectWithValue }) => {
    try {
      const response = await userService.getFavoritePlacesWithDetails();
        console.log("API /favorites/details response:", response.data);
      return response.data;
    } catch (error) {
      return rejectWithValue(error.response?.data?.message || 'Failed to fetch favorite places');
    }
  }
);

// Slice
const userSlice = createSlice({
  name: 'user',
  initialState: {
    favoritePlaceIds: [],
    favoritePlaces: [],
    loading: false,
    error: null
  },
  reducers: {
    clearFavorites: (state) => {
      state.favoritePlaceIds = [];
      state.favoritePlaces = [];
    }
  },
  extraReducers: (builder) => {
    builder
      // Fetch favorite place IDs
      .addCase(fetchFavoritePlaces.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchFavoritePlaces.fulfilled, (state, action) => {
        state.loading = false;
        state.favoritePlaceIds = action.payload;
      })
      .addCase(fetchFavoritePlaces.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload;
        toast.error(action.payload);
      })
      // Add favorite place
      .addCase(addFavoritePlace.fulfilled, (state, action) => {
        if (!state.favoritePlaceIds.includes(action.payload)) {
          state.favoritePlaceIds.push(action.payload);
        }
      })
      .addCase(addFavoritePlace.rejected, (state, action) => {
        state.error = action.payload;
        toast.error(action.payload);
      })
      // Remove favorite place
      .addCase(removeFavoritePlace.fulfilled, (state, action) => {
        state.favoritePlaceIds = state.favoritePlaceIds.filter(id => id !== action.payload);
      })
      .addCase(removeFavoritePlace.rejected, (state, action) => {
        state.error = action.payload;
        toast.error(action.payload);
      })
      // Fetch favorite places with details
      .addCase(fetchFavoritePlacesWithDetails.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchFavoritePlacesWithDetails.fulfilled, (state, action) => {
        state.loading = false;
        state.favoritePlaces = action.payload;
      })
      .addCase(fetchFavoritePlacesWithDetails.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload;
        toast.error(action.payload);
      });
  }
});

export const { clearFavorites } = userSlice.actions;
export default userSlice.reducer;