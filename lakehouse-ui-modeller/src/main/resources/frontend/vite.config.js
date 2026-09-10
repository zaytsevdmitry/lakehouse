import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { resolve } from 'node:path';

export default defineConfig({
  plugins: [react()],
  base: '/',
  build: {
    outDir: resolve(__dirname, '../static'),
    emptyOutDir: true,
  },
  server: {
    port: 5173,
    proxy: {
      '/v1_0': 'http://localhost:8094',
    },
  },
});