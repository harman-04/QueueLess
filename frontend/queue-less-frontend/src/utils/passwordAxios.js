//src/utils/passwordAxios.js
import axios from 'axios';

const passwordAxios = axios.create({
  baseURL: 'https://localhost:8443/api/password',
  headers: {
    'Content-Type': 'application/json',
  },
});

export default passwordAxios;
