import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

export default defineConfig({
  plugins: [
    react({
      babel: {
        plugins: [['babel-plugin-react-compiler']],
      },
    }),
  ],
  server :{
    host:true
  },
  resolve: {
    alias: {
      "@enums" : path.resolve(__dirname,'./src/assets/js/enums'),
      '@styles': path.resolve(__dirname, './src/styles'),
      '@components': path.resolve(__dirname, './src/components'),
      '@assets': path.resolve(__dirname, './src/assets'),
      '@context': path.resolve(__dirname,'./src/context'),
      "@apiBase": path.resolve(__dirname,'./src/api-base')
    },
  },
});
