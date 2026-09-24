import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// /api 로 시작하는 요청을 스프링(8080)으로 넘긴다 → 개발 중 CORS 설정이 필요 없다.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
});