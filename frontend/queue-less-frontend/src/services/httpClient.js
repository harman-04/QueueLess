// src/services/httpClient.js

import axios from "axios";

const httpClient = axios.create({
  baseURL: "http://localhost:8080", // your Spring Boot backend
  headers: {
    "Content-Type": "application/json",
  },
});

export default httpClient;
