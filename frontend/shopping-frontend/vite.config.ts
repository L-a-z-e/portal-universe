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
  console.log('🔧 [Shopping] Portal Remote URL:', env.VITE_PORTAL_SHELL_REMOTE_URL || '(using default)')

  // base 설정 제거 - 상대 경로 사용 (blog-frontend와 동일)
  // vite-plugin-federation이 import.meta.url 기준으로 chunk 경로를 동적 해석
  return {

    plugins: [
      react(),
      federation({
        name: 'shopping-frontend',
        filename: 'remoteEntry.js',
        remotes: {
          portal: env.VITE_PORTAL_SHELL_REMOTE_URL,
          blog: env.VITE_BLOG_REMOTE_URL,
          shopping: env.VITE_SHOPPING_REMOTE_URL
        },
        exposes: {
          './bootstrap': './src/bootstrap.tsx'
        },
        shared: ['react', 'react-dom'],
      }),
    ],

    resolve: {
      alias: {
        '@portal/design-system-react/styles': resolve(
          __dirname,
          '../design-system-react/src/styles/index.css'
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
      postcss: './postcss.config.js',
    },

    server: {
      port: 30002,
      host: '0.0.0.0',
      cors: true,
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
      minify: false,
      cssCodeSplit: true,
      sourcemap: false,
      outDir: 'dist',
    },
  }
})