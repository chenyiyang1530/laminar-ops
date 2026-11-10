import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  cacheDir: fileURLToPath(new URL('./node_modules/.vite/admin', import.meta.url)),
  optimizeDeps: {
    include: ['vue', 'vue-router', 'pinia', 'axios', 'element-plus', '@element-plus/icons-vue', 'echarts']
  },
  resolve: {
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) }
  },
  server: {
    host: '0.0.0.0',
    port: 5173,
    proxy: {
      '/api': { target: 'http://127.0.0.1:8080', changeOrigin: true },
      '/upload': { target: 'http://127.0.0.1:8080', changeOrigin: true }
    }
  }
})
