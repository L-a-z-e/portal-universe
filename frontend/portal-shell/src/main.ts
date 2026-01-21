// portal-shell/src/main.ts

import { type ComponentPublicInstance, createApp } from 'vue'
import { createPinia } from 'pinia';
import './style.css'
import router from './router'
import { useAuthStore } from './store/auth';
import '@portal/design-system-vue/style.css';
import './style.css';
import AppVue from './App.vue';

const app = createApp(AppVue);
const pinia = createPinia();

// ✅ Global error handler (protect portal-shell)
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

// ✅ Promise rejection handler
window.addEventListener('unhandledrejection', (event) => {
  console.error('❌ Unhandled promise rejection:', event.reason);
  event.preventDefault();
});

app.use(router);
app.use(pinia);
app.mount('#app');

// ✅ Auth initialization (Direct JWT)
const authStore = useAuthStore();

authStore.checkAuth()
  .then(() => {
    console.log('✅ Auth check completed');
  })
  .catch(err => {
    console.error('⚠️ Auth initialization failed:', err);
  });
