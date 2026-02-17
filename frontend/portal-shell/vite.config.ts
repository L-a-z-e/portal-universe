import {defineConfig, loadEnv} from 'vite'
import vue from '@vitejs/plugin-vue'
import federation from "@originjs/vite-plugin-federation";
import { resolve } from 'path';

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  // .env 파일 로드
  const env = loadEnv(mode, process.cwd(), '');

  console.log('🔧 [Vite Config] Building for mode:', mode);
  console.log('🔧 [Vite Config] Blog Remote URL:', env.VITE_BLOG_REMOTE_URL);
  console.log('🔧 [Vite Config] Shopping Remote URL:', env.VITE_SHOPPING_REMOTE_URL);
  console.log('🔧 [Vite Config] Prism Remote URL:', env.VITE_PRISM_REMOTE_URL);
  console.log('🔧 [Vite Config] Admin Remote URL:', env.VITE_ADMIN_REMOTE_URL);
  console.log('🔧 [Vite Config] Drive Remote URL:', env.VITE_DRIVE_REMOTE_URL);
  console.log('🔧 [Vite Config] Seller Remote URL:', env.VITE_SELLER_REMOTE_URL);

  return {
    plugins: [
      vue(),
      federation({
        name: 'portal',
        filename: 'shellEntry.js',
        remotes: {
          blog: env.VITE_BLOG_REMOTE_URL,
          shopping: env.VITE_SHOPPING_REMOTE_URL,
          prism: env.VITE_PRISM_REMOTE_URL,
          admin: env.VITE_ADMIN_REMOTE_URL,
          drive: env.VITE_DRIVE_REMOTE_URL,
          seller: env.VITE_SELLER_REMOTE_URL,
        },
        exposes: {
          './api': './src/api/index.ts',
          './stores': './src/store/index.ts',
        },
        shared: ['vue', 'pinia', 'axios'],
      })
    ],
    resolve: {
      alias: {
        '@': resolve(__dirname, 'src'),
        '@portal/design-vue/style.css': resolve(__dirname, '../design-vue/dist/design-system.css')
      }
    },
    server: {
      port: 30000,
      proxy: {
        '/auth-service': {
          target: env.VITE_API_BASE_URL || 'http://localhost:8080',
          changeOrigin: true,
        },
        '/api': {
          target: env.VITE_API_BASE_URL || 'http://localhost:8080',
          changeOrigin: true,
        },
      }
    },
    preview: {
      port: 30000,
      cors: true,
      proxy: {
        '/auth-service': {
          target: env.VITE_API_BASE_URL || 'http://localhost:8080',
          changeOrigin: true,
        },
        '/api': {
          target: env.VITE_API_BASE_URL || 'http://localhost:8080',
          changeOrigin: true,
        },
      }
    },
    build: {
      minify: false,
      target: 'esnext',
    },
    esbuild: mode === 'production' ? {
      pure: ['console.log', 'console.debug', 'console.group', 'console.groupEnd'],
    } : undefined,
  }
})