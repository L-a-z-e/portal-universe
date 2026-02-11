import './style.css';
import { createApp } from 'vue';
import App from './App.vue';
import { createStandaloneDriveRouter } from './router';
import { createPinia } from "pinia";

const isEmbedded = window.__POWERED_BY_PORTAL_SHELL__ === true;
const mode = isEmbedded ? 'EMBEDDED' : 'STANDALONE';

console.log(`[Drive] Detected mode: ${mode}`);

if (isEmbedded) {
  console.log('[Drive] Waiting for Portal Shell to mount...');
} else {
  console.group('[Drive] Starting in STANDALONE mode');

  const appElement = document.querySelector('#app') as HTMLElement | null;

  if (!appElement) {
    console.error('[Drive] #app element not found!');
    console.groupEnd();
    throw new Error('[Drive] Mount target not found');
  }

  try {
    const app = createApp(App);
    const pinia = createPinia();
    const router = createStandaloneDriveRouter();

    app.use(pinia);
    app.use(router);
    app.mount(appElement);

    console.log('[Drive] Mounted successfully');
    console.log(`   URL: ${window.location.href}`);
    console.log(`   Route: ${router.currentRoute.value.path}`);
  } catch (err) {
    console.error('[Drive] Mount failed:', err);
  }

  console.groupEnd();
}

declare global {
  interface Window {
    __POWERED_BY_PORTAL_SHELL__?: boolean;
  }
}
