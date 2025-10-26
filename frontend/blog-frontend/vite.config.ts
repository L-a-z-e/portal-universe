import {defineConfig, loadEnv} from 'vite'
import vue from '@vitejs/plugin-vue'
import federation from "@originjs/vite-plugin-federation";
import { resolve } from 'path';

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  // 현재 빌드 모드(dev, docker, k8s)에 맞는 .env 파일을 로드합니다.
  const env = loadEnv(mode, process.cwd(), '')
  console.log('🔧 [Vite Config] Building for mode:', mode);
  console.log('🔧 [Vite Config] Portal Remote URL:', env.VITE_PORTAL_SHELL_REMOTE_URL);

  return {
    plugins: [
      vue(),
      federation({
        // --- Remote 설정 (자신을 Remote로 정의) ---
        name: 'blog_remote', // 이 Remote 앱의 고유 이름
        filename: 'remoteEntry.js', // 이 Remote 앱의 진입점 파일 이름

        // --- Exposes (외부에 노출할 모듈) ---
        exposes: {
          // './bootstrap': './src/bootstrap.ts'
          // Portal Shell이 이 앱을 동적으로 로드하고 마운트할 수 있도록 `bootstrap.ts` 파일을 노출합니다.
          // Shell에서는 `import('blog_remote/bootstrap')` 형태로 이 파일을 가져올 수 있습니다.
          './bootstrap': './src/bootstrap.ts'
        },

        // --- Remotes (참조할 다른 Remote) ---
        remotes: {
          // Portal Shell이 노출하는 모듈(authStore 등)을 사용하기 위해 Shell을 Remote로 등록합니다.
          portal_shell: env.VITE_PORTAL_SHELL_REMOTE_URL,
        },

        // --- Shared (공유할 의존성) ---
        // 셸과 동일한 버전의 라이브러리를 공유하여 중복 로드를 방지합니다.
        shared: ['vue', 'pinia']
      })
    ],
    resolve: {
      alias: {
        '@portal/design-system/style.css': resolve(__dirname, '../design-system/dist/design-system.css')
      }
    },
    server: {
      port: 30001,
      cors: true
    },
    preview: {
      port: 30001,
      cors: true,
    },
    build: {
      minify: false,
      target: 'esnext',
    }
  }
});
