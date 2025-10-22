import {type ComponentPublicInstance, createApp} from 'vue'
import { createPinia } from "pinia";
import './style.css'
import App from './App.vue'
import router from './router'
import { useAuthStore } from "./store/auth.ts";
import userManager from "./services/authService.ts";
import '@portal/design-system/style.css';
import './style.css';
const app = createApp(App);
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

// ✅ Auth 초기화 에러 처리
userManager.getUser()
  .then(user => {
    if (user && user.access_token) {
      authStore.setUser(user);
    }
  })
  .catch(err => {
    console.error('⚠️ Auth initialization failed:', err);
    // portal-shell은 계속 동작
  });