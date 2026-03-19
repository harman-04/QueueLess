import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import fs from 'fs'

export default defineConfig({
  plugins: [react()],
  define: {
    global: 'window',
  },
  server: {
    https: {
      key: fs.readFileSync('ssl/key.pem'), // adjust path if needed
      cert: fs.readFileSync('ssl/cert.pem'),
    },
    host: true,
    proxy: {
      '/api': {
        target: 'https://localhost:8443',
        changeOrigin: true,
        secure: false, // accept backend's self‑signed cert
      },
      '/uploads': 'https://localhost:8443',
    },
  },
})