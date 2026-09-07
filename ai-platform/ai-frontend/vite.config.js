import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/auth': 'http://localhost:8082',
      '/userinfo': 'http://localhost:8082',
      '/api/agent': {
        target: 'http://localhost:8083',
        changeOrigin: true
      },
      '/api/jobs': {
        target: 'http://localhost:8084',
        changeOrigin: true
      },
      '/api': {
        target: 'http://localhost:8083',
        changeOrigin: true
      }
    }
  }
})