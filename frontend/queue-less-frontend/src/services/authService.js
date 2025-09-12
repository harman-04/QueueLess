// src/services/authService.js
import axiosInstance from '../utils/axiosInstance';

export const authService = {
  login: (credentials) => axiosInstance.post('/auth/login', credentials),
  register: (userData) => axiosInstance.post('/auth/register', userData),
  getProfile: () => axiosInstance.get('/auth/profile'),
  updateProfile: (userData) => axiosInstance.put('/user/profile', userData),
  uploadProfileImage: (formData) => axiosInstance.post('/auth/upload-profile-image', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  }),
  changePassword: (passwordData) => axiosInstance.put('/user/password', passwordData),
  // New method for account deletion
  deleteAccount: () => axiosInstance.delete('/user/account'),
};