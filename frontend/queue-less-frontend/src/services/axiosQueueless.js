// src/services/httpClient.js
import axios from 'axios';

export const apiConnector = axios.create({
  baseURL: 'https://localhost:8443',
  headers: {
    'Content-Type': 'application/json',
  },
});
