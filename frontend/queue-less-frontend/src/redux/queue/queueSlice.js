// src/redux/queue/queueSlice.js
import { createSlice } from '@reduxjs/toolkit';

const initialState = {
  data: null,
  publicQueues: [],
  connected: false,
  error: null,
};

const queueSlice = createSlice({
  name: 'queue',
  initialState,
  reducers: {
    updateQueue: (state, action) => {
      // Normalize the queue data to handle both 'active' and 'isActive' fields
      if (action.payload) {
        state.data = {
          ...action.payload,
          isActive: action.payload.active !== undefined ? action.payload.active : action.payload.isActive
        };
      } else {
        state.data = null;
      }
    },
    connectionSuccess: (state) => {
      state.connected = true;
      state.error = null;
    },
    connectionFailure: (state) => {
      state.connected = false;
      state.error = 'WebSocket connection failed';
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase('WS_CONNECT', (state) => {
        state.loading = true;
        state.connected = false;
        state.error = null;
      })
      .addCase('WS_CONNECTED', (state) => {
        state.connected = true;
        console.log("✅ WebSocket connected!");
      })
      .addCase('WS_DISCONNECTED', (state) => {
        state.connected = false;
        state.data = null;
        console.log("❌ WebSocket disconnected!");
      })
      .addCase('WS_MESSAGE_RECEIVED', (state, action) => {
        // Normalize the queue data
        state.data = {
          ...action.payload,
          isActive: action.payload.active !== undefined ? action.payload.active : action.payload.isActive
        };
        state.loading = false;
        state.error = null;
        console.log("📥 Queue update:", state.data);
      })
      .addCase('QUEUE_FETCH_SUCCESS', (state, action) => {
        // Normalize the queue data
        state.data = {
          ...action.payload,
          isActive: action.payload.active !== undefined ? action.payload.active : action.payload.isActive
        };
        state.loading = false;
      })
      .addCase('QUEUE_FETCH_ERROR', (state, action) => {
        state.error = action.payload;
        state.loading = false;
      });
  },
});

export const { updateQueue, connectionSuccess, connectionFailure } = queueSlice.actions;
export default queueSlice.reducer;