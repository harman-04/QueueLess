import axios from 'axios';

const passwordAxios = axios.create({
  baseURL: 'http://localhost:8080/api/password',
  headers: {
    'Content-Type': 'application/json',
  },
});

export default passwordAxios;
