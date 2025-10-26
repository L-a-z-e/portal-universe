/// <reference types="vitest/config" />
import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import { resolve } from 'path';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { storybookTest } from '@storybook/addon-vitest/vitest-plugin';

const dirname = typeof __dirname !== 'undefined' ? __dirname : path.dirname(fileURLToPath(import.meta.url));

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  build: {
    // 라이브러리 모드로 빌드하도록 설정합니다.
    lib: {
      entry: resolve(__dirname, 'src/index.ts'), // 라이브러리의 진입점 파일
      name: 'PortalDesignSystem', // UMD 빌드 시 사용될 전역 변수 이름
      fileName: 'index', // 출력될 파일의 기본 이름 (index.js, index.cjs)
      formats: ['es', 'cjs'] // 생성할 모듈 포맷 (ES Module, CommonJS)
    },
    rollupOptions: {
      // 라이브러리에 포함시키지 않을 외부 의존성을 명시합니다.
      // Vue는 이 라이브러리를 사용하는 앱에 이미 설치되어 있을 것이므로, 번들에 포함하지 않습니다.
      external: ['vue'],
      output: {
        // UMD 빌드 시, 외부 의존성(vue)이 어떤 전역 변수를 참조할지 설정합니다.
        globals: {
          vue: 'Vue'
        },
        // CSS 파일의 출력 이름을 'design-system.css'로 고정합니다.
        assetFileNames: assetInfo => {
          if (assetInfo.name && assetInfo.name.endsWith('.css')) return 'design-system.css';
          return assetInfo.name || '';
        }
      }
    },
    cssCodeSplit: false, // 모든 CSS를 하나의 파일로 번들링합니다.
    outDir: 'dist', // 빌드 결과물이 생성될 디렉토리
    emptyOutDir: true // 빌드 시 outDir을 먼저 비웁니다.
  },
  test: {
    // Vitest 설정 (Storybook 연동)
    projects: [{
      extends: true,
      plugins: [
        storybookTest({
          configDir: path.join(dirname, '.storybook')
        })],
      test: {
        name: 'storybook',
        browser: {
          enabled: true,
          headless: true,
          provider: 'playwright',
          instances: [{
            browser: 'chromium'
          }]
        },
        setupFiles: ['.storybook/vitest.setup.ts']
      }
    }]
  }
});
