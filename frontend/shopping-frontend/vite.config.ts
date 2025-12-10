/// <reference types="vite/client" />
import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import federation from '@originjs/vite-plugin-federation'
import { resolve } from 'path'
import path from 'path'

/**
 * Shopping Frontend Vite Config
 *
 * Hub & Spoke 패턴을 지원하는 Remote 앱
 * - Host(Vue Portal)에서 Props로 상태 주입
 * - React 앱이 Props 변화에 자동 반응
 */
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  console.log('🔧 [Shopping] Building for mode:', mode)

  return {
    plugins: [
      react(),
      federation({
        name: 'shopping-frontend',
        filename: 'remoteEntry.js',

        // ✅ Bootstrap만 expose
        // mount 함수가 Host와의 통신 채널 역할
        exposes: {
          './bootstrap': './src/bootstrap.tsx'
        },

        // ✅ Host와 공유할 라이브러리만
        // 상태관리는 각 앱이 독립적으로 관리
        shared: ['react', 'react-dom'],
      }),
    ],

    resolve: {
      alias: {
        '@portal/design-system/style.css': resolve(
          __dirname,
          '../design-system/dist/design-system.css'
        ),
        '@': path.resolve(__dirname, './src'),
        '@components': path.resolve(__dirname, './src/components'),
        '@pages': path.resolve(__dirname, './src/pages'),
        '@stores': path.resolve(__dirname, './src/stores'),
        '@hooks': path.resolve(__dirname, './src/hooks'),
        '@types': path.resolve(__dirname, './src/types'),
      },
    },

    css: {
      // postcss: './postcss.config.js'
    },

    server: {
      port: 30002,
      host: '0.0.0.0',    // Docker 환경 호환
      cors: true,          // Host 통신 허용
      open: false,
    },

    preview: {
      port: 30002,
      host: '0.0.0.0',
      cors: true,
      open: false,
    },

    build: {
      target: 'esnext',
      minify: false,       // 디버깅 용이
      cssCodeSplit: true,
      sourcemap: false,
      outDir: 'dist',
    },
  }
})