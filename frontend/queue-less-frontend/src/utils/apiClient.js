// src/utils/apiClient.js
import axios from 'axios';

const apiClient = axios.create({
  baseURL: 'http://localhost:8080/api', // change if your backend is deployed
  headers: {
    'Content-Type': 'application/x-www-form-urlencoded',
  },
  withCredentials: true, // if you are using cookies/session (optional, remove if not needed)
});

// You can also intercept requests/responses if needed
apiClient.interceptors.request.use(
  (config) => {
    // Add Authorization token if needed from Redux or LocalStorage
    // Example: 
    // const token = localStorage.getItem('authToken');
    // if (token) {
    //   config.headers.Authorization = `Bearer ${token}`;
    // }
    return config;
  },
  (error) => Promise.reject(error)
);

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    // You can handle global errors here if needed
    return Promise.reject(error);
  }
);

export default apiClient;
