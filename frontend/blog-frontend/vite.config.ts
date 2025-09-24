import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import federation from "@originjs/vite-plugin-federation";

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    federation({ //federation plugin add
      name: 'blog_frontend_remote',
      filename: 'remoteEntry.js',
      exposes: {
        // 외부에서 BlogApp 이라는 이름으로 import하면, 이 프로젝트의 './src/App.vue' 파일을 노출
        './BlogApp': './src/App.vue',
      },
      shared: ['vue'] // Vue 라이브러리는 셸과 공유하여 중복 로드를 피함
    })
  ],
  base: './',
  server: {
    port: 5082,
    cors: true
  },
  build: {
    target: 'esnext', // 브라우저 호환성 설정
    // rollupOptions: {
    //   output: {
    //     // 모든 빌드 결과물의 경로에서 해시값([hash])을 제거하고
    //     // 고정된 파일 이름을 사용하도록 설정합니다.
    //     entryFileNames: `assets/[name].js`,
    //     chunkFileNames: `assets/[name].js`,
    //     assetFileNames: `assets/[name].[ext]`
    //   }
    // }
  }
})
