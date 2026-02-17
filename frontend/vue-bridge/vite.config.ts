import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import dts from 'vite-plugin-dts';
import { resolve } from 'path';

/**
 * @portal/vue-bridge - Vite Library Mode Config
 *
 * Portal Shell(Vue Host)과 Vue Remote 앱 사이의 Bridge 라이브러리
 * - Composables (usePortalAuth, usePortalTheme)
 * - Sync utilities (getPortalAuthState)
 * - Lifecycle (disposePortalAuth, disposePortalTheme)
 */
export default defineConfig({
  plugins: [
    vue(),
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
      name: 'PortalVueBridge',
      formats: ['es', 'cjs'],
      fileName: (format) => `index.${format === 'es' ? 'js' : 'cjs'}`,
    },
    rollupOptions: {
      external: [
        'vue',
        // Module Federation 런타임 모듈
        'portal/stores',
        'portal/api',
      ],
      output: {
        globals: {
          vue: 'Vue',
        },
      },
    },
    sourcemap: true,
    minify: false,
  },
});
