import {defineConfig, loadEnv} from 'vite'
import vue from '@vitejs/plugin-vue'
import federation from "@originjs/vite-plugin-federation";
import { resolve } from 'path';

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  // 현재 빌드 모드(dev, docker, k8s)에 맞는 .env 파일을 로드합니다.
  const env = loadEnv(mode, process.cwd(), '');

  console.log('🔧 [Vite Config] Building for mode:', mode);
  console.log('🔧 [Vite Config] Blog Remote URL:', env.VITE_BLOG_REMOTE_URL);

  return {
    plugins: [
      vue(),
      federation({
        // --- Host (Shell) 설정 ---
        name: 'portal', // 현재 셸(호스트) 앱의 고유 이름
        filename: 'shellEntry.js', // 셸 앱의 진입점 파일 이름 (거의 사용되지 않음)

        // --- Remotes (로드할 마이크로 프론트엔드) ---
        remotes: {
          // 'key': 'url' 형식으로 원격 앱을 등록합니다.
          // key: 원격 앱을 참조할 때 사용할 이름 (예: import ... from 'blog_remote/...')
          // url: 원격 앱의 remoteEntry.js 파일 주소
          blog_remote: env.VITE_BLOG_REMOTE_URL,
        },

        // --- Exposes (외부에 노출할 모듈) ---
        // 다른 원격 앱이 이 셸 앱의 모듈을 가져다 쓸 수 있도록 노출합니다.
        // 예: 다른 앱에서 import authStore from 'portal/authStore' 형태로 사용 가능
        exposes: {
          './authStore': './src/store/auth.ts',
          './themeStore': './src/store/theme.ts',
        },

        // --- Shared (공유할 의존성) ---
        // 여러 마이크로 프론트엔드 간에 중복으로 로드하지 않고 공유할 라이브러리를 지정합니다.
        // 이를 통해 전체 애플리케이션의 번들 크기를 최적화할 수 있습니다.
        shared: ['vue', 'pinia'],
      })
    ],
    resolve: {
      alias: {
        // design-system의 빌드 결과물(CSS)에 대한 별칭을 설정합니다.
        '@portal/design-system/style.css': resolve(__dirname, '../design-system/dist/design-system.css')
      }
    },
    server: {
      port: 30000
    },
    preview: {
      port: 30000,
      cors: true,
    },
    build: {
      minify: false,
      target: 'esnext'
    }
  }
})