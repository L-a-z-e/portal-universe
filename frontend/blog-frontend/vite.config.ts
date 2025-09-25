import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import federation from "@originjs/vite-plugin-federation";

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    federation({ //federation plugin add
      name: 'blog_remote',
      filename: 'remoteEntry.js',
      exposes: {
        // 외부에서 BlogApp 이라는 이름으로 import하면, 이 프로젝트의 './src/App.vue' 파일을 노출
        './BlogApp': './src/App.vue',
        './bootstrap': './src/bootstrap.ts'
      },
      shared: ['vue'] // Vue 라이브러리는 셸과 공유하여 중복 로드를 피함
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
