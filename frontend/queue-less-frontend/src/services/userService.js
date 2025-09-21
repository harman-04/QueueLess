// src/services/userService.js
import axiosInstance from '../utils/axiosInstance';

export const userService = {
  // Get favorite places
  getFavoritePlaces: () => axiosInstance.get('/user/favorites'),
  
  // Add a place to favorites
  addFavoritePlace: (placeId) => axiosInstance.post(`/user/favorites/${placeId}`),
  
  // Remove a place from favorites
  removeFavoritePlace: (placeId) => axiosInstance.delete(`/user/favorites/${placeId}`),
  
  // Get favorite places with details
  getFavoritePlacesWithDetails: () => axiosInstance.get('/user/favorites/details')
};