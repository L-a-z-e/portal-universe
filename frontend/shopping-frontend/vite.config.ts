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
        exposes: {
          './bootstrap': './src/bootstrap.tsx'
        },
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
      postcss: './postcss.config.js'
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
      rollupOptions: {
        // Portal Shell 모듈은 런타임에 Module Federation으로 제공됨
        external: ['portal/themeStore', 'portal/authStore', 'portal/apiClient'],
      },
    },
  }
})