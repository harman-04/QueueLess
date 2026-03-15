import axiosInstance from '../utils/axiosInstance';

export const notificationPreferenceService = {
  // Get all preferences for the authenticated user
  getMyPreferences: () => axiosInstance.get('/notifications/preferences/my'),

  // Get preference for a specific queue
  getPreferenceForQueue: (queueId) => axiosInstance.get(`/notifications/preferences/queue/${queueId}`),

  // Create or update preference for a queue
  updatePreference: (queueId, data) => axiosInstance.put(`/notifications/preferences/queue/${queueId}`, data),

  // Delete preference for a queue
  deletePreference: (queueId) => axiosInstance.delete(`/notifications/preferences/queue/${queueId}`),
};