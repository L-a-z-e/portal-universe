// admin-frontend/src/bootstrap.ts
import './style.css';
import { createApp, type App as VueApp } from 'vue';
import App from './App.vue';
import type { Router } from 'vue-router';
import { createAdminRouter } from './router';
import { createPinia } from 'pinia';
import { disposePortalAuth } from '@portal/vue-bridge';

export type MountOptions = {
  initialPath?: string;
  onNavigate?: (path: string) => void;
}

export type AdminAppInstance = {
  router: Router;
  onParentNavigate: (path: string) => void;
  unmount: () => void;
  onActivated?: () => void;
  onDeactivated?: () => void;
}

export function mountAdminApp(
  el: HTMLElement,
  options: MountOptions = {}
): AdminAppInstance {
  console.group('[Admin] Mounting app in EMBEDDED mode');

  (window as any).__POWERED_BY_PORTAL_SHELL__ = true;

  if (!el) {
    console.error('[Admin] Mount element is null!');
    console.groupEnd();
    throw new Error('[Admin] Mount element is required');
  }

  const { initialPath, onNavigate } = options;

  const app: VueApp = createApp(App);
  const pinia = createPinia();
  app.use(pinia);

  const router = createAdminRouter('/');
  app.use(router);

  const targetPath = initialPath || '/';
  router.push(targetPath).catch(err => {
    console.error('[Admin] Initial navigation failed:', err);
  });

  router.afterEach((to, from) => {
    if (to.path !== from.path) {
      onNavigate?.(to.path);
    }
  });

  app.mount(el);
  console.log('[Admin] App mounted successfully');
  console.groupEnd();

  return {
    router,

    onParentNavigate: (path: string) => {
      if (router.currentRoute.value.path !== path) {
        router.push(path).catch(err => {
          console.error('[Admin] Parent navigation failed:', err);
        });
      }
    },

    onActivated: () => {
      document.documentElement.setAttribute('data-service', 'admin');
    },

    onDeactivated: () => {},

    unmount: () => {
      console.group('[Admin] Unmounting app');

      // Portal auth 구독 해제
      disposePortalAuth();

      try {
        app.unmount();
      } catch (err) {
        console.error('[Admin] App unmount failed:', err);
      }
      try {
        el.innerHTML = '';
        if (document.documentElement.getAttribute('data-service') === 'admin') {
          document.documentElement.removeAttribute('data-service');
        }
      } catch (err) {
        console.error('[Admin] Cleanup failed:', err);
      }
      console.groupEnd();
    }
  };
}
