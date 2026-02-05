import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import dts from 'vite-plugin-dts';
import { resolve } from 'path';

/**
 * @portal/react-bridge - Vite Library Mode Config
 *
 * Portal Shell(Vue)과 React 앱 사이의 Bridge 라이브러리
 * - Store Adapters (Auth, Theme)
 * - Hooks (usePortalAuth, usePortalTheme)
 * - Provider (PortalBridgeProvider)
 * - API Client Factory
 */
export default defineConfig({
  plugins: [
    react(),
    dts({
      insertTypesEntry: true,
      include: ['src'],
    }),
  ],
  resolve: {
    alias: {
      '@': resolve(__dirname, './src'),
    },
  },
  build: {
    lib: {
      entry: resolve(__dirname, 'src/index.ts'),
      name: 'PortalReactBridge',
      formats: ['es', 'cjs'],
      fileName: (format) => `index.${format === 'es' ? 'js' : 'cjs'}`,
    },
    rollupOptions: {
      external: [
        'react',
        'react-dom',
        'react-dom/client',
        'react/jsx-runtime',
        'react-router-dom',
        'axios',
        // Module Federation 런타임 모듈
        'portal/stores',
        'portal/api',
      ],
      output: {
        globals: {
          react: 'React',
          'react-dom': 'ReactDOM',
          'react-dom/client': 'ReactDOMClient',
          'react/jsx-runtime': 'jsxRuntime',
          'react-router-dom': 'ReactRouterDOM',
          axios: 'axios',
        },
      },
    },
    sourcemap: true,
    minify: false,
  },
});
