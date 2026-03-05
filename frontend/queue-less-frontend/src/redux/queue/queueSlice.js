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
});

export const { updateQueue, connectionSuccess, connectionFailure } = queueSlice.actions;
export default queueSlice.reducer;