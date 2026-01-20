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

  // 환경별 base 설정 - Module Federation chunk 파일 로드 경로
  // vite-plugin-federation은 Vite의 base 옵션을 사용하여 remoteEntry.js 내 chunk 경로를 결정함
  const basePaths: Record<string, string> = {
    dev: 'http://localhost:30002/',
    docker: 'http://shopping-frontend/',
    k8s: 'http://shopping-frontend.portal-universe.svc.cluster.local/',
  }

  // 환경별 Portal Shell remote URL (themeStore 등 import용)
  const portalRemoteUrls: Record<string, string> = {
    dev: 'http://localhost:30000/assets/shellEntry.js',
    docker: 'http://portal-shell/assets/shellEntry.js',
    k8s: 'http://portal-shell.portal-universe.svc.cluster.local/assets/shellEntry.js',
  }
  const portalRemoteUrl = env.VITE_PORTAL_SHELL_REMOTE_URL || portalRemoteUrls[mode] || portalRemoteUrls.dev

  return {
    base: basePaths[mode] || 'http://localhost:30002/',

    plugins: [
      react(),
      federation({
        name: 'shopping-frontend',
        filename: 'remoteEntry.js',
        remotes: {
          portal: portalRemoteUrl,
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