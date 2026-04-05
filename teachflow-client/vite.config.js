import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src')
    }
  },
  server: {
    // 👇👇👇 新增这两行 👇👇👇
    host: '0.0.0.0',       // 允许电脑外部的网络（局域网/穿透）访问
    allowedHosts: true,    // 允许 cpolar 生成的域名访问（解决 403 报错）
    // 👆👆👆 新增这两行 👆👆👆

    // 下面的 proxy 保持你原来的样子完全不动
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      }
    }
  }
})