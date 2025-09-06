// src/redux/placeSlice.js
import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import { placeService } from '../services/placeService';
import axiosInstance from '../utils/axiosInstance';
// In the fetchPlaces async thunk
export const fetchPlaces = createAsyncThunk(
  'places/fetchAll',
  async (_, { rejectWithValue }) => {
    try {
      const response = await placeService.getAll();
      return response.data;
    } catch (error) {
      return rejectWithValue(error); // ✅ normalized
    }
  }
);


export const fetchPlaceById = createAsyncThunk(
  'places/fetchById',
  async (id, { rejectWithValue }) => {
    try {
      const response = await placeService.getById(id);
      return response.data;
    } catch (error) {
      return rejectWithValue(error);
    }
  }
);

export const createPlace = createAsyncThunk(
  'places/create',
  async (placeData, { rejectWithValue }) => {
    try {
      const response = await placeService.create(placeData);
      return response.data;
    } catch (error) {
      return rejectWithValue(error);
    }
  }
);

export const updatePlace = createAsyncThunk(
  'places/update',
  async ({ id, placeData }, { rejectWithValue }) => {
    try {
      const response = await placeService.update(id, placeData);
      return response.data;
    } catch (error) {
      return rejectWithValue(error);
    }
  }
);

export const fetchPlacesByAdmin = createAsyncThunk(
  'places/fetchByAdmin',
  async (adminId, { rejectWithValue }) => {
    try {
      const response = await axiosInstance.get(`/places/admin/${adminId}`, {
        headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
      });
      return response.data;
    } catch (error) {
      return rejectWithValue(error);
    }
  }
);


// Add this to your placeSlice.js
export const fetchMyPlaces = createAsyncThunk(
  'places/fetchMy',
  async (_, { rejectWithValue, getState }) => {
    try {
      const { auth } = getState();
      const response = await axiosInstance.get('/places/admin/my-places', {
        headers: { Authorization: `Bearer ${auth.token}` }
      });
      return response.data;
    } catch (error) {
      return rejectWithValue(error);
    }
  }
);


export const deletePlace = createAsyncThunk(
  'places/delete',
  async (id, { rejectWithValue }) => {
    try {
      await placeService.delete(id);
      return id;
    } catch (error) {
      return rejectWithValue(error);
    }
  }
);


const placeSlice = createSlice({
  name: 'places',
  initialState: {
    items: [],
    currentPlace: null,
    loading: false,
    error: null
  },
  reducers: {
    clearCurrentPlace: (state) => {
      state.currentPlace = null;
    }
  },
  extraReducers: (builder) => {
    builder

    .addCase(fetchMyPlaces.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchMyPlaces.fulfilled, (state, action) => {
        state.loading = false;
        state.items = action.payload;
      })
      .addCase(fetchMyPlaces.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload;
      })
      .addCase(fetchPlaces.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchPlaces.fulfilled, (state, action) => {
        state.loading = false;
        state.items = action.payload;
      })
      .addCase(fetchPlaces.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload;
      })
      .addCase(fetchPlaceById.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchPlaceById.fulfilled, (state, action) => {
        state.loading = false;
        state.currentPlace = action.payload;
      })
      .addCase(fetchPlaceById.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload;
      })
      .addCase(createPlace.fulfilled, (state, action) => {
        state.items.push(action.payload);
      })
      .addCase(updatePlace.fulfilled, (state, action) => {
        const index = state.items.findIndex(item => item.id === action.payload.id);
        if (index !== -1) {
          state.items[index] = action.payload;
        }
        if (state.currentPlace && state.currentPlace.id === action.payload.id) {
          state.currentPlace = action.payload;
        }
      })
      .addCase(deletePlace.fulfilled, (state, action) => {
        state.items = state.items.filter(item => item.id !== action.payload);
      })
      .addCase(fetchPlacesByAdmin.pending, (state) => {
  state.loading = true;
  state.error = null;
})
.addCase(fetchPlacesByAdmin.fulfilled, (state, action) => {
  state.loading = false;
  state.items = action.payload;
})
.addCase(fetchPlacesByAdmin.rejected, (state, action) => {
  state.loading = false;
  state.error = action.payload;
})
;
  }
});

export const { clearCurrentPlace } = placeSlice.actions;
export default placeSlice.reducer;