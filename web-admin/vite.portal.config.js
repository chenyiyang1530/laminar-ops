import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  root: fileURLToPath(new URL('./src-portal', import.meta.url)),
  plugins: [vue()],
  cacheDir: fileURLToPath(new URL('./node_modules/.vite/portal', import.meta.url)),
  optimizeDeps: {
    include: ['vue', 'vue-router', 'pinia', 'axios', 'element-plus', '@element-plus/icons-vue']
  },
  resolve: {
    alias: { '@': fileURLToPath(new URL('./src-portal', import.meta.url)) }
  },
  server: {
    host: '0.0.0.0',
    port: 5174,
    proxy: {
      '/api': { target: 'http://127.0.0.1:8080', changeOrigin: true },
      '/upload': { target: 'http://127.0.0.1:8080', changeOrigin: true }
    }
  }
})
