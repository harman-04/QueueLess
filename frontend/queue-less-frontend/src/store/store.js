import { configureStore } from '@reduxjs/toolkit';
import authReducer from '../redux/authSlice';
import queueReducer from '../redux/queue/queueSlice';
import placeReducer from '../redux/placeSlice';
import serviceReducer from '../redux/serviceSlice';
import searchReducer from '../redux/searchSlice';
import userReducer from '../redux/userSlice';
import adminAnalyticsReducer from '../redux/adminAnalyticsSlice';
import providerAnalyticsReducer from '../redux/providerAnalyticsSlice'; 
import userAnalyticsReducer from '../redux/userAnalyticsSlice';

const store = configureStore({
  reducer: {
    auth: authReducer,
    queue: queueReducer,
    places: placeReducer,
    services: serviceReducer,
    search: searchReducer,
    user: userReducer,
    adminAnalytics: adminAnalyticsReducer,
    providerAnalytics: providerAnalyticsReducer, 
    userAnalytics: userAnalyticsReducer,
    
  },
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware({
      serializableCheck: false,
    }),
});

export default store;