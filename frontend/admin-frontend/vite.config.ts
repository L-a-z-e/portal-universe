import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import federation from '@originjs/vite-plugin-federation'
import { resolve } from 'path'
import path from 'path'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')

  console.log('[Vite Config] Building admin-frontend for mode:', mode);
  console.log('[Vite Config] Portal Remote URL:', env.VITE_PORTAL_SHELL_REMOTE_URL);

  return {
    plugins: [
      vue(),
      federation({
        name: 'admin',
        filename: 'remoteEntry.js',
        remotes: {
          portal: env.VITE_PORTAL_SHELL_REMOTE_URL,
        },
        exposes: {
          './bootstrap': './src/bootstrap.ts'
        },
        shared: ['vue', 'pinia', 'axios']
      })
    ],
    resolve: {
      alias: {
        '@portal/design-system-vue/style.css': resolve(__dirname, '../design-system-vue/dist/design-system.css'),
        '@': path.resolve(__dirname, './src')
      }
    },
    server: {
      port: 30004,
      cors: true,
    },
    preview: {
      port: 30004,
      cors: true,
    },
    build: {
      minify: false,
      target: 'esnext',
    },
    esbuild: mode === 'production' ? {
      pure: ['console.log', 'console.debug', 'console.group', 'console.groupEnd'],
    } : undefined,
  }
})
