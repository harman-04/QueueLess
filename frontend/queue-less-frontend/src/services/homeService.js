import axiosInstance from '../utils/axiosInstance';

export const getRecentFeedback = (limit = 5) => 
  axiosInstance.get(`/feedback/recent?limit=${limit}`);

export const getTopRatedPlaces = (limit = 3) => 
  axiosInstance.get(`/places/top-rated?limit=${limit}`);

export const getPublicStats = () => 
  axiosInstance.get('/public/stats');

export const getLiveStats = () => axiosInstance.get('/public/live-stats');