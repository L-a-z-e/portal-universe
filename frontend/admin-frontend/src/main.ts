// admin-frontend/src/main.ts
import './style.css';
import { createApp } from 'vue';
import App from './App.vue';
import { createStandaloneAdminRouter } from './router';
import { createPinia } from 'pinia';
import { setupErrorHandler } from '@portal/design-system-vue';

const isEmbedded = window.__POWERED_BY_PORTAL_SHELL__ === true;

if (isEmbedded) {
  console.log('[Admin] Waiting for Portal Shell to mount...');
} else {
  console.group('[Admin] Starting in STANDALONE mode');

  const appElement = document.querySelector('#app') as HTMLElement | null;

  if (!appElement) {
    console.error('[Admin] #app element not found!');
    console.groupEnd();
    throw new Error('[Admin] Mount target not found');
  }

  try {
    const app = createApp(App);
    const pinia = createPinia();
    const router = createStandaloneAdminRouter();

    setupErrorHandler(app, { moduleName: 'Admin' });
    app.use(pinia);
    app.use(router);
    app.mount(appElement);

    console.log('[Admin] Mounted successfully');
  } catch (err) {
    console.error('[Admin] Mount failed:', err);
  }

  console.groupEnd();
}

declare global {
  interface Window {
    __POWERED_BY_PORTAL_SHELL__?: boolean;
  }
}
