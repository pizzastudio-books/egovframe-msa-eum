import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'node:path';

// 이음 프론트엔드 Vite 설정
// - @ alias로 src를 가리킨다(공식 simple-react 관례).
// - dev 서버에서 /api 요청을 백엔드(8080)로 프록시해 로컬 CORS를 우회한다(6장).
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './src/test/setup.js',
  },
});
