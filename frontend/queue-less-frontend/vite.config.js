import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import basicSsl from '@vitejs/plugin-basic-ssl' // Import the plugin

export default defineConfig({
  plugins: [
    react(),
    basicSsl(), // Add the plugin here
  ],
  define: {
    global: 'window',
  },
  server: {
    https: true, // Enable HTTPS
    host: true,
    proxy: {
      '/api': {
        target: 'https://localhost:8443', // Your backend's HTTPS URL
        changeOrigin: true,
        secure: false, // This is crucial for accepting your self-signed backend cert
      },
    },
  },
})