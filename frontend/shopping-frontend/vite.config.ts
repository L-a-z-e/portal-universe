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

  // Module Federation remote 앱의 chunk가 올바른 URL에서 로드되도록 base 설정
  // React lazy() + code-splitting 사용 시 필수 (blog-frontend는 단일 번들이라 불필요)
  // .env 미설정 시 기본 로컬 URL fallback (빌드 시 federation 플러그인 크래시 방지)
  const DEFAULT_REMOTES = {
    portal: 'http://localhost:30000/assets/shellEntry.js',
    blog: 'http://localhost:30001/assets/remoteEntry.js',
    shopping: 'http://localhost:30002/assets/remoteEntry.js',
  }

  return {
    base: env.VITE_BASE_URL,

    plugins: [
      react(),
      federation({
        name: 'shopping-frontend',
        filename: 'remoteEntry.js',
        remotes: {
          portal: env.VITE_PORTAL_SHELL_REMOTE_URL || DEFAULT_REMOTES.portal,
          blog: env.VITE_BLOG_REMOTE_URL || DEFAULT_REMOTES.blog,
          shopping: env.VITE_SHOPPING_REMOTE_URL || DEFAULT_REMOTES.shopping,
        },
        exposes: {
          './bootstrap': './src/bootstrap.tsx'
        },
        shared: ['react', 'react-dom', 'react-dom/client', 'axios', 'react-router-dom'],
      }),
    ],

    resolve: {
      alias: {
        '@portal/design-react/style.css': resolve(
          __dirname,
          '../design-react/src/styles/index.css'
        ),
        '@portal/design-react': resolve(
          __dirname,
          '../design-react/src/index.ts'
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
      minify: 'esbuild',
      cssCodeSplit: true,
      sourcemap: false,
      outDir: 'dist',
    },

    esbuild: mode === 'production' ? {
      pure: ['console.log', 'console.debug', 'console.group', 'console.groupEnd'],
    } : undefined,
  }
})