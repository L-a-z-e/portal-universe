import {defineConfig, loadEnv} from 'vite'
import vue from '@vitejs/plugin-vue'
import federation from "@originjs/vite-plugin-federation";

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  // .env 파일 로드
  const env = loadEnv(mode, process.cwd(), '')

  return {
    plugins: [
      vue(),
      federation({
        name: 'blog_remote',
        remotes: {
          portal_shell: env.VITE_PORTAL_SHELL_REMOTE_URL || "http://localhost:5000/assets/shellEntry.js",
          shopping_remote: env.VITE_SHOP_REMOTE_URL || "http://localhost:5002/assets/remoteEntry.js",
        },
        filename: 'remoteEntry.js',
        exposes: {
          './bootstrap': './src/bootstrap.ts'
        },
        shared: ['vue', 'pinia']
      })
    ],
    server: {
      port: 5001,
      cors: true
    },
    build: {
      minify: false,
      target: 'esnext',
    }
  }
});