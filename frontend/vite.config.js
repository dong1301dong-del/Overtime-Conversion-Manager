import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 开发期通过代理访问后端 8080；生产构建产物由 Spring Boot 托管于 /api 同域。
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: 'dist',
    assetsDir: 'assets'
  }
})
