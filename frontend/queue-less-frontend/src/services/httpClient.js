// src/services/httpClient.js

import axios from "axios";

const httpClient = axios.create({
  baseURL: "https://localhost:8443", // your Spring Boot backend
  headers: {
    "Content-Type": "application/json",
  },
});

export default httpClient;
