import {type ComponentPublicInstance, createApp} from 'vue'
import { createPinia } from "pinia";
import './style.css'
import router from './router'
import { useAuthStore } from "./store/auth.ts";
import userManager from "./services/authService.ts";
import '@portal/design-system/style.css';
import './style.css';
import AppVue from './App.vue';

const app = createApp(AppVue);
const pinia = createPinia();

/**
 * 전역 Vue 에러 핸들러를 설정합니다.
 * 컴포넌트 렌더링 또는 생명주기 훅에서 발생하는 예외를 처리합니다.
 * 이는 특정 리모트 앱의 오류가 전체 셸 앱을 중단시키는 것을 방지하는 중요한 보호 장치입니다.
 */
app.config.errorHandler = (
  err: unknown,
  instance: ComponentPublicInstance | null,
  info: string
) => {
  console.error('❌ Global error caught:', err);
  console.error('   Error info:', info);

  if (instance) {
    console.error('   Component:', instance.$options.name);
    console.error('   Props:', instance.$props);
  }
};

/**
 * 처리되지 않은 Promise 거부(rejection)를 감지하는 전역 핸들러입니다.
 * 비동기 코드에서 발생하는 예외를 처리합니다.
 */
window.addEventListener('unhandledrejection', (event) => {
  console.error('❌ Unhandled promise rejection:', event.reason);
  event.preventDefault();
});

app.use(router);
app.use(pinia);
app.mount('#app');

const authStore = useAuthStore();

/**
 * 애플리케이션 시작 시, OIDC 사용자 세션을 확인하고 복원합니다.
 * 페이지 새로고침 시 로그인 상태를 유지하는 역할을 합니다.
 */
userManager.getUser()
  .then(user => {
    if (user && user.access_token) {
      authStore.setUser(user);
    }
  })
  .catch(err => {
    // 초기 인증 확인 실패는 치명적인 오류가 아니므로, 콘솔에만 에러를 기록하고 앱은 계속 실행합니다.
    console.error('⚠️ Auth initialization failed:', err);
  });
