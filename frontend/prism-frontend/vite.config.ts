/// <reference types="vite/client" />
import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import federation from '@originjs/vite-plugin-federation'
import { resolve } from 'path'
import path from 'path'

/**
 * Prism Frontend Vite Config
 *
 * AI Agent Orchestration Kanban Board
 * - Module Federation Remote 앱
 * - Host(Vue Portal)에서 Props로 상태 주입
 */
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  console.log('🔧 [Prism] Building for mode:', mode)
  console.log('🔧 [Prism] Portal Remote URL:', env.VITE_PORTAL_SHELL_REMOTE_URL || '(using default)')

  const DEFAULT_REMOTES = {
    portal: 'http://localhost:30000/assets/shellEntry.js',
  }

  return {
    base: env.VITE_BASE_URL,

    plugins: [
      react(),
      federation({
        name: 'prism',
        filename: 'remoteEntry.js',
        remotes: {
          portal: env.VITE_PORTAL_SHELL_REMOTE_URL || DEFAULT_REMOTES.portal,
        },
        exposes: {
          './bootstrap': './src/bootstrap.tsx'
        },
        shared: ['react', 'react-dom', 'react-dom/client'],
      }),
    ],

    resolve: {
      alias: {
        '@portal/design-system-react/style.css': resolve(
          __dirname,
          '../design-system-react/src/styles/index.css'
        ),
        '@portal/design-system-react': resolve(
          __dirname,
          '../design-system-react/src/index.ts'
        ),
        '@': path.resolve(__dirname, './src'),
        '@components': path.resolve(__dirname, './src/components'),
        '@pages': path.resolve(__dirname, './src/pages'),
        '@stores': path.resolve(__dirname, './src/stores'),
        '@hooks': path.resolve(__dirname, './src/hooks'),
        '@router': path.resolve(__dirname, './src/router'),
        '@types': path.resolve(__dirname, './src/types'),
        '@services': path.resolve(__dirname, './src/services'),
      },
    },

    css: {
      postcss: './postcss.config.js',
    },

    server: {
      port: 30003,
      host: '0.0.0.0',
      cors: true,
      open: false,
    },

    preview: {
      port: 30003,
      host: '0.0.0.0',
      cors: true,
      open: false,
    },

    build: {
      target: 'esnext',
      minify: mode === 'production' ? 'esbuild' : false,
      cssCodeSplit: true,
      sourcemap: false,
      outDir: 'dist',
    },
  }
})
