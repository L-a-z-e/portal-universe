import './style.css';
import { createApp, type App as VueApp } from 'vue';
import App from './App.vue';
import type { Router } from 'vue-router';
import { createDriveRouter, logRouterInfo } from "./router";
import { createPinia } from "pinia";
import { disposePortalAuth } from '@portal/vue-bridge';

export type MountOptions = {
  initialPath?: string;
  onNavigate?: (path: string) => void;
}

export type DriveAppInstance = {
  router: Router;
  onParentNavigate: (path: string) => void;
  unmount: () => void;
  onActivated?: () => void;
  onDeactivated?: () => void;
}

export function mountDriveApp(
  el: HTMLElement,
  options: MountOptions = {}
): DriveAppInstance {
  console.group('[Drive] Mounting app in EMBEDDED mode');

  (window as any).__POWERED_BY_PORTAL_SHELL__ = true;

  if (!el) {
    console.error('[Drive] Mount element is null!');
    console.groupEnd();
    throw new Error('[Drive] Mount element is required');
  }


  const { initialPath, onNavigate } = options;

  const app: VueApp = createApp(App);

  const pinia = createPinia();
  app.use(pinia);

  const router = createDriveRouter('/');
  app.use(router);

  logRouterInfo(router);

  const targetPath = initialPath || '/';

  router.push(targetPath).catch(err => {
    console.error('[Drive] Initial navigation failed:', err);
  });

  router.afterEach((to, from) => {
    if (to.path !== from.path) {
      onNavigate?.(to.path);
    }
  });

  app.mount(el);
  console.groupEnd();

  return {
    router,

    onParentNavigate: (path: string) => {

      if (router.currentRoute.value.path !== path) {
        router.push(path).catch(err => {
          console.error('[Drive] Parent navigation failed:', err);
        });
      }
    },

    onActivated: () => {
      document.documentElement.setAttribute('data-service', 'drive');
    },

    onDeactivated: () => {
    },

    unmount: () => {
      console.group('[Drive] Unmounting app');

      // Portal auth 구독 해제
      disposePortalAuth();

      try {
        app.unmount();
      } catch (err) {
        console.error('[Drive] App unmount failed:', err);
      }

      try {
        el.innerHTML = '';

        if (document.documentElement.getAttribute('data-service') === 'drive') {
          document.documentElement.removeAttribute('data-service');
        }

      } catch (err) {
        console.error('[Drive] Cleanup failed:', err);
      }

      console.groupEnd();
    }
  };
}
