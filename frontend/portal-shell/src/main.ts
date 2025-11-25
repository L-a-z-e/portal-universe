// portal-shell/src/main.ts

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

// ✅ 전역 에러 핸들러 추가 (portal-shell 보호)
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

// ✅ Promise rejection 핸들러 추가
window.addEventListener('unhandledrejection', (event) => {
  console.error('❌ Unhandled promise rejection:', event.reason);
  event.preventDefault();
});

app.use(router);
app.use(pinia);
app.mount('#app');

const authStore = useAuthStore();

// Auth 초기화 - 만료된 토큰 검증 및 정리
userManager.getUser()
  .then(async user => {
    // 토큰이 존재하는 경우
    if (user && user.access_token) {
      // 토큰 만료 여부 확인
      if (user.expired) {
        console.warn('⚠️ Token expired on initialization, clearing storage...');

        // localStorage에서 만료된 토큰 제거
        await userManager.removeUser();

        // authStore 초기화
        authStore.logout();

        console.log('✅ Expired token cleared successfully');
        return;
      }

      // 유효한 토큰인 경우에만 설정
      console.log('✅ Valid token found, setting user...');
      authStore.setUser(user);
    } else {
      console.log('ℹ️ No token found, user not authenticated');
    }
  })
  .catch(async err => {
    console.error('⚠️ Auth initialization failed:', err);

    // 에러 발생 시에도 localStorage 정리
    try {
      await userManager.removeUser();
      authStore.logout();
      console.log('✅ Storage cleared after initialization error');
    } catch (cleanupErr) {
      console.error('❌ Failed to cleanup storage:', cleanupErr);
    }
  });