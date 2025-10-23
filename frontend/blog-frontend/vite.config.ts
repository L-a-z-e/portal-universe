import {defineConfig, loadEnv} from 'vite'
import vue from '@vitejs/plugin-vue'
import federation from "@originjs/vite-plugin-federation";
import { resolve } from 'path';

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  // .env 파일 로드
  const env = loadEnv(mode, process.cwd(), '')
  console.log('🔧 [Vite Config] Building for mode:', mode);
  console.log('🔧 [Vite Config] Portal Remote URL:', env.VITE_PORTAL_SHELL_REMOTE_URL);

  return {
    plugins: [
      vue(),
      federation({
        name: 'blog_remote',
        remotes: {
          portal_shell: env.VITE_PORTAL_SHELL_REMOTE_URL,
          // shopping_remote: env.VITE_SHOP_REMOTE_URL,
        },
        filename: 'remoteEntry.js',
        exposes: {
          './bootstrap': './src/bootstrap.ts'
        },
        shared: ['vue', 'pinia']
      })
    ],
    resolve: {
      alias: {
        '@portal/design-system/style.css': resolve(__dirname, '../design-system/dist/design-system.css')
      }
    },
    server: {
      port: 30001,
      cors: true
    },
    preview: {
      port: 30001,
      cors: true,
    },
    build: {
      minify: false,
      target: 'esnext',
    }
  }
});