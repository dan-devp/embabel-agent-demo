import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/chat': { target: 'http://localhost:8088', changeOrigin: true },
      '/knowledge/': { target: 'http://localhost:8088', changeOrigin: true },
      '/memory/': { target: 'http://localhost:8088', changeOrigin: true },
      '/research/': { target: 'http://localhost:8088', changeOrigin: true },
      '/collaboration/': { target: 'http://localhost:8088', changeOrigin: true },
      '/action/': { target: 'http://localhost:8088', changeOrigin: true }
    }
  }
})
