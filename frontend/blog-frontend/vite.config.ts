import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import federation from "@originjs/vite-plugin-federation";

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    federation({ //federation plugin add
      name: 'blog_remote',
      remotes: {
        portal_shell: "http://localhost:50000/assets/shellEntry.js"
      },
      filename: 'remoteEntry.js',
      exposes: {
        './bootstrap': './src/bootstrap.ts'
      },
      shared: ['vue', 'pinia'] // Vue 라이브러리는 셸과 공유하여 중복 로드를 피함
    })
  ],
  // base: '/',
  server: {
    port: 5082,
    cors: true
  },
  build: {
    minify:false,
    target: 'esnext', // 브라우저 호환성 설정
  }
})
