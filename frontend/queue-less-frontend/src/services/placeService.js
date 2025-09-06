// src/services/placeService.js
import axiosInstance from '../utils/axiosInstance';

export const placeService = {
  getAll: () => axiosInstance.get('/places'),
  getById: (id) => axiosInstance.get(`/places/${id}`),
  create: (placeData) => axiosInstance.post('/places', placeData),
  update: (id, placeData) => axiosInstance.put(`/places/${id}`, placeData),
  delete: (id) => axiosInstance.delete(`/places/${id}`),
  getByAdmin: (adminId) => axiosInstance.get(`/places/admin/${adminId}`),
  getNearby: (longitude, latitude, radius) => 
    axiosInstance.get(`/places/nearby?longitude=${longitude}&latitude=${latitude}&radius=${radius}`),
  getByType: (type) => axiosInstance.get(`/places/type/${type}`)
};