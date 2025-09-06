// src/utils/axiosInstance.js
import axios from 'axios';
import store from '../store/store';

const axiosInstance = axios.create({
  baseURL: 'http://localhost:8080/api',
});

// Request interceptor
axiosInstance.interceptors.request.use(
  (config) => {
    const token = store.getState().auth.token;
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    if (config.data && !config.headers['Content-Type']) {
      config.headers['Content-Type'] = 'application/json';
    }

    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor
axiosInstance.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response) {
      const { status, data } = error.response;

      // Normalize backend ApiError
      const normalizedError = {
        status,
        error: data?.error || 'Error',
        message: data?.message || 'An unexpected error occurred',
        path: data?.path,
        timestamp: data?.timestamp,
      };

      // Handle auth errors
      if (status === 401) {
        store.dispatch({ type: 'auth/logout' });
        window.location.href = '/login';
      }

      return Promise.reject(normalizedError);
    }

    return Promise.reject({
      status: 0,
      error: 'Network Error',
      message: error.message || 'Unable to connect to server',
    });
  }
);

export default axiosInstance;
