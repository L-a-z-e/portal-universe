import {defineConfig, loadEnv} from 'vite'
import vue from '@vitejs/plugin-vue'
import federation from "@originjs/vite-plugin-federation";

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  // .env 파일 로드
  const env = loadEnv(mode, process.cwd(), '')
  console.log('🔧 [Vite Config] Building for mode:', mode);
  console.log('🔧 [Vite Config] Blog Remote URL:', env.VITE_BLOG_REMOTE_URL);

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