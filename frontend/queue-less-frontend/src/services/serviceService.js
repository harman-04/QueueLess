// src/services/serviceService.js
import axiosInstance from '../utils/axiosInstance';

export const serviceService = {
  getAll: () => axiosInstance.get('/services'),
  getById: (id) => axiosInstance.get(`/services/${id}`),
  create: (serviceData) => axiosInstance.post('/services', serviceData),
  update: (id, serviceData) => axiosInstance.put(`/services/${id}`, serviceData),
  delete: (id) => axiosInstance.delete(`/services/${id}`),
  getByPlace: (placeId) => axiosInstance.get(`/services/place/${placeId}`)
};