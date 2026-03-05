// src/services/adminService.js
import axiosInstance from '../utils/axiosInstance';

export const adminService = {
  getStats: () => axiosInstance.get('/admin/stats'),
  getProviders: () => axiosInstance.get('/admin/providers'),
  getQueuesEnhanced: () => axiosInstance.get('/admin/queues/enhanced'),
  getPaymentsEnhanced: () => axiosInstance.get('/admin/payments/enhanced'),
  // New analytics endpoints
  getTokensOverTime: (days = 30) => axiosInstance.get(`/admin/analytics/tokens-over-time?days=${days}`),
  getBusiestHours: () => axiosInstance.get('/admin/analytics/busiest-hours'),
};