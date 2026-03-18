import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

export default ({ mode }) => {

  // in dev, proxy /api to the Spring Boot backend on :8080.
  // in prod, the SPA is served from Azure blob storage and
  // the API is on the same Front Door hostname — no proxy needed.
  const devProxy = {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
      secure: false,
      rewrite: (path) => path.replace(/^\/api/, ''),
      cookieDomainRewrite: 'localhost'
    }
  };

  return defineConfig({
    plugins: [
    react({
      babel: {
        plugins: [['babel-plugin-react-compiler']],
      },
    }),
  ],
    server: {
      host: true,
      proxy: mode === 'development' ? devProxy : undefined,
    },
  resolve: {
    alias: {
      "@enums" : path.resolve(__dirname,'./src/assets/js/enums'),
      '@styles': path.resolve(__dirname, './src/styles'),
      '@components': path.resolve(__dirname, './src/components'),
      '@assets': path.resolve(__dirname, './src/assets'),
      '@context': path.resolve(__dirname,'./src/context'),
      '@hooks': path.resolve(__dirname,'./src/hooks'),
    },
  },
  });
}
