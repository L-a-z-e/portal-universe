// portal-shell/src/main.ts

import { createApp } from 'vue'
import { createPinia } from 'pinia';
import './style.css'
import router from './router'
import { useAuthStore } from './store/auth';
import '@portal/design-system-vue/style.css';
import './style.css';
import AppVue from './App.vue';
import { setupErrorHandler } from '@portal/design-system-vue';

const app = createApp(AppVue);
const pinia = createPinia();

setupErrorHandler(app, { moduleName: 'Portal' });

app.use(pinia);

// ✅ Auth initialization (반드시 router 등록 전에 완료)
const authStore = useAuthStore();
const authReady = authStore.checkAuth()
  .then(() => {
    console.log('✅ Auth check completed');
  })
  .catch(err => {
    console.error('⚠️ Auth initialization failed:', err);
  });

// Auth 초기화 Promise를 전역으로 노출 (router guard에서 사용)
(window as any).__AUTH_READY__ = authReady;

app.use(router);
app.mount('#app');
