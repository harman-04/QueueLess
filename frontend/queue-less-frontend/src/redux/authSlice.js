// src/redux/authSlice.js
import { createSlice } from '@reduxjs/toolkit';

// Get initial state from localStorage
const getInitialState = () => {
  const token = localStorage.getItem('token');
  const role = localStorage.getItem('role');
  const id = localStorage.getItem('userId');
  const name = localStorage.getItem('name');
  const profileImageUrl = localStorage.getItem('profileImageUrl');
  const placeId = localStorage.getItem('placeId');
  const isVerified = localStorage.getItem('isVerified') === 'true';
  const preferences = JSON.parse(localStorage.getItem('preferences') || 'null');
  const ownedPlaceIds = JSON.parse(localStorage.getItem('ownedPlaceIds') || '[]');

  return {
    token: token || null,
    role: role || null,
    id: id || null,
    name: name || null,
    profileImageUrl: profileImageUrl || null,
    placeId: placeId || null,
    isVerified: isVerified || false,
    preferences: preferences || {
      emailNotifications: true,
      smsNotifications: false,
      language: 'en',
      defaultSearchRadius: 5,
      darkMode: false,
      favoritePlaceIds: []
    },
    ownedPlaceIds: ownedPlaceIds || []
  };
};

const authSlice = createSlice({
  name: 'auth',
  initialState: getInitialState(),
  reducers: {
    loginSuccess: (state, action) => {
      const { token, role, userId, name, profileImageUrl, placeId, isVerified, preferences, ownedPlaceIds } = action.payload;
      state.token = token;
      state.role = role;
      state.id = userId;
      state.name = name;
      state.profileImageUrl = profileImageUrl || null;
      state.placeId = placeId || null;
      state.isVerified = isVerified || false;
      state.preferences = preferences || {
        emailNotifications: true,
        smsNotifications: false,
        language: 'en',
        defaultSearchRadius: 5,
        darkMode: false,
        favoritePlaceIds: []
      };
      state.ownedPlaceIds = ownedPlaceIds || [];
      
      // Save to localStorage
      localStorage.setItem('token', token);
      localStorage.setItem('role', role);
      localStorage.setItem('userId', userId);
      localStorage.setItem('name', name);
      if (profileImageUrl) localStorage.setItem('profileImageUrl', profileImageUrl);
      if (placeId) localStorage.setItem('placeId', placeId);
      localStorage.setItem('isVerified', isVerified ? 'true' : 'false');
      localStorage.setItem('preferences', JSON.stringify(state.preferences));
      localStorage.setItem('ownedPlaceIds', JSON.stringify(state.ownedPlaceIds));
    },
    updateProfile: (state, action) => {
      const { name, profileImageUrl, placeId, isVerified, preferences, ownedPlaceIds } = action.payload;
      if (name) {
        state.name = name;
        localStorage.setItem('name', name);
      }
      if (profileImageUrl !== undefined) {
        state.profileImageUrl = profileImageUrl;
        if (profileImageUrl) {
          localStorage.setItem('profileImageUrl', profileImageUrl);
        } else {
          localStorage.removeItem('profileImageUrl');
        }
      }
      if (placeId !== undefined) {
        state.placeId = placeId;
        if (placeId) {
          localStorage.setItem('placeId', placeId);
        } else {
          localStorage.removeItem('placeId');
        }
      }
      if (isVerified !== undefined) {
        state.isVerified = isVerified;
        localStorage.setItem('isVerified', isVerified ? 'true' : 'false');
      }
      if (preferences !== undefined) {
        state.preferences = { ...state.preferences, ...preferences };
        localStorage.setItem('preferences', JSON.stringify(state.preferences));
      }
      if (ownedPlaceIds !== undefined) {
        state.ownedPlaceIds = ownedPlaceIds;
        localStorage.setItem('ownedPlaceIds', JSON.stringify(ownedPlaceIds));
      }
    },
    updatePreferences: (state, action) => {
      state.preferences = { ...state.preferences, ...action.payload };
      localStorage.setItem('preferences', JSON.stringify(state.preferences));
    },
    logout: (state) => {
      state.token = null;
      state.role = null;
      state.id = null;
      state.name = null;
      state.profileImageUrl = null;
      state.placeId = null;
      state.isVerified = false;
      state.preferences = {
        emailNotifications: true,
        smsNotifications: false,
        language: 'en',
        defaultSearchRadius: 5,
        darkMode: false,
        favoritePlaceIds: []
      };
      state.ownedPlaceIds = [];
      
      // Clear localStorage
      localStorage.clear();
    },
  },
});

export const { loginSuccess, updateProfile, updatePreferences, logout } = authSlice.actions;
export default authSlice.reducer;