import { configureStore } from '@reduxjs/toolkit';

import authReducer from '../redux/authSlice';

import queueReducer from '../redux/queue/queueSlice'; // Import your new queue slice

import websocketMiddleware from '../redux/queue/websocketMiddleware'; // Import your new middleware


const store = configureStore({

  reducer: {

    auth: authReducer,

    queue: queueReducer, // Add the new queue reducer to your store

  },

  middleware: (getDefaultMiddleware) =>

    getDefaultMiddleware({

      serializableCheck: false, // You'll need this to handle the non-serializable WebSocket objects

    }).concat(websocketMiddleware),

});


export default store;
