import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import federation from "@originjs/vite-plugin-federation";

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    federation({
      name: 'portal_shell_host',
      remotes: {
        blog_remote: "http://localhost:5082/assets/remoteEntry.js",
      },
      shared: ['vue'],

    })
  ],
  server: {
    port: 50000,
    cors: true,
    // 👇 프록시 설정을 추가
    proxy: {
      '/blog_remote': {
        target: 'http://localhost:5082', // 실제 목적지
        changeOrigin: true, // 출처(Origin) 헤더를 목적지에 맞게 변경
        rewrite: (path) => {
          return path.replace(/blog_remote/, '/assets');
        }
      }
    }
  },
  build: {
    minify:false,
    target: 'esnext'
  }
})
