// src/redux/serviceSlice.js
import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import { serviceService } from '../services/serviceService';

// In the fetchServices async thunk
export const fetchServices = createAsyncThunk(
  'services/fetchAll',
  async (_, { rejectWithValue }) => {
    try {
      const response = await serviceService.getAll(); // This will call /api/services
      return response.data;
    } catch (error) {
      return rejectWithValue(error);
    }
  }
);

export const fetchServiceById = createAsyncThunk(
  'services/fetchById',
  async (id, { rejectWithValue }) => {
    try {
      const response = await serviceService.getById(id);
      return response.data;
    } catch (error) {
      return rejectWithValue(error);
    }
  }
);

export const createService = createAsyncThunk(
  'services/create',
  async (serviceData, { rejectWithValue }) => {
    try {
      const response = await serviceService.create(serviceData);
      return response.data;
    } catch (error) {
      return rejectWithValue(error);
    }
  }
);

export const updateService = createAsyncThunk(
  'services/update',
  async ({ id, serviceData }, { rejectWithValue }) => {
    try {
      const response = await serviceService.update(id, serviceData);
      return response.data;
    } catch (error) {
      return rejectWithValue(error);
    }
  }
);

export const deleteService = createAsyncThunk(
  'services/delete',
  async (id, { rejectWithValue }) => {
    try {
      await serviceService.delete(id);
      return id;
    } catch (error) {
      return rejectWithValue(error);
    }
  }
);

export const fetchServicesByPlace = createAsyncThunk(
  'services/fetchByPlace',
  async (placeId, { rejectWithValue }) => {
    try {
      const response = await serviceService.getByPlace(placeId);
      return { placeId, services: response.data };
    } catch (error) {
      return rejectWithValue(error);
    }
  }
);

const serviceSlice = createSlice({
  name: 'services',
  initialState: {
    items: [],
    servicesByPlace: {},
    currentService: null,
    loading: false,
    error: null
  },
  reducers: {
    clearCurrentService: (state) => {
      state.currentService = null;
    }
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchServices.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchServices.fulfilled, (state, action) => {
        state.loading = false;
        state.items = action.payload;
      })
      .addCase(fetchServices.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload;
      })
      .addCase(fetchServiceById.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchServiceById.fulfilled, (state, action) => {
        state.loading = false;
        state.currentService = action.payload;
      })
      .addCase(fetchServiceById.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload;
      })
      .addCase(createService.fulfilled, (state, action) => {
        state.items.push(action.payload);
      })
      .addCase(updateService.fulfilled, (state, action) => {
        const index = state.items.findIndex(item => item.id === action.payload.id);
        if (index !== -1) {
          state.items[index] = action.payload;
        }
        if (state.currentService && state.currentService.id === action.payload.id) {
          state.currentService = action.payload;
        }
      })
      .addCase(deleteService.fulfilled, (state, action) => {
        state.items = state.items.filter(item => item.id !== action.payload);
      })
      .addCase(fetchServicesByPlace.fulfilled, (state, action) => {
        state.servicesByPlace[action.payload.placeId] = action.payload.services;
      });
  }
});

export const { clearCurrentService } = serviceSlice.actions;
export default serviceSlice.reducer;