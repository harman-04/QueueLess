import axios from 'axios';
import store from '../store/store';

const axiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'https://localhost:8443/api',
});

// Request interceptor (unchanged)
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

// Response interceptor – updated
axiosInstance.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response) {
      const { status, data } = error.response;

      // Handle token expiry
      if (status === 401 && data?.message?.includes('JWT') && data?.message?.includes('expired')) {
        store.dispatch({ type: 'auth/logout' });
        window.location.href = '/login';
        return Promise.reject(new Error('Token expired. Please login again.'));
      }

      // Normalize error: data can be a string or an object
      let errorMessage = 'An unexpected error occurred';
      if (typeof data === 'string') {
        errorMessage = data;
      } else if (data?.message) {
        errorMessage = data.message;
      }

      const normalizedError = {
        status,
        error: data?.error || 'Error',
        message: errorMessage,
        path: data?.path,
        timestamp: data?.timestamp,
      };
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