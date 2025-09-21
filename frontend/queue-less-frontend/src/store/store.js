// src/store/store.js
import { configureStore } from '@reduxjs/toolkit';
import authReducer from '../redux/authSlice';
import queueReducer from '../redux/queue/queueSlice';
import placeReducer from '../redux/placeSlice';
import serviceReducer from '../redux/serviceSlice';
import searchReducer from '../redux/searchSlice'; // Make sure this import exists
import userReducer from '../redux/userSlice'; // Add this line
import websocketMiddleware from '../redux/queue/websocketMiddleware';

const store = configureStore({
  reducer: {
    auth: authReducer,
    queue: queueReducer,
    places: placeReducer,
    services: serviceReducer,
    search: searchReducer,
     user: userReducer,
  },
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware({
      serializableCheck: false,
    }).concat(websocketMiddleware),
});

export default store;