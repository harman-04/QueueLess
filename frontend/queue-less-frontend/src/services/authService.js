import axiosInstance from '../utils/axiosInstance';

export const authService = {
  login: (credentials) => axiosInstance.post('/auth/login', credentials),
  register: (userData) => axiosInstance.post('/auth/register', userData),
  getProfile: () => axiosInstance.get('/auth/profile'),
  updateProfile: (userData) => axiosInstance.put('/user/profile', userData),
  // Upload profile image – expects FormData
  uploadProfileImage: (formData) => axiosInstance.post('/user/profile/image', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  }),
  changePassword: (passwordData) => axiosInstance.put('/user/password', passwordData),
  deleteAccount: () => axiosInstance.delete('/user/account'),
};