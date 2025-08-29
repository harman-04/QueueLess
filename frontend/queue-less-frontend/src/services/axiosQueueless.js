// src/services/httpClient.js
import axios from 'axios';

export const apiConnector = axios.create({
  baseURL: 'http://localhost:8080',
  headers: {
    'Content-Type': 'application/json',
  },
});
