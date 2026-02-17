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

  console.log('Mount target:', el.tagName, el.className || '(no class)');
  console.log('Options:', options);

  const { initialPath, onNavigate } = options;

  const app: VueApp = createApp(App);

  const pinia = createPinia();
  app.use(pinia);

  const router = createDriveRouter('/');
  app.use(router);

  logRouterInfo(router);

  const targetPath = initialPath || '/';
  console.log(`[Drive] Navigating to: ${targetPath}`);

  router.push(targetPath).catch(err => {
    console.error('[Drive] Initial navigation failed:', err);
  });

  router.afterEach((to, from) => {
    if (to.path !== from.path) {
      console.log(`[Drive] Route changed: ${from.path} -> ${to.path}`);
      onNavigate?.(to.path);
    }
  });

  app.mount(el);
  console.log('[Drive] App mounted successfully');
  console.groupEnd();

  return {
    router,

    onParentNavigate: (path: string) => {
      console.log(`[Drive] Received navigation from parent: ${path}`);

      if (router.currentRoute.value.path !== path) {
        router.push(path).catch(err => {
          console.error('[Drive] Parent navigation failed:', err);
        });
      }
    },

    onActivated: () => {
      console.log('[Drive] App activated (keep-alive)');
      document.documentElement.setAttribute('data-service', 'drive');
    },

    onDeactivated: () => {
      console.log('[Drive] App deactivated (keep-alive)');
    },

    unmount: () => {
      console.group('[Drive] Unmounting app');

      // Portal auth 구독 해제
      disposePortalAuth();

      try {
        app.unmount();
        console.log('[Drive] App unmounted successfully');
      } catch (err) {
        console.error('[Drive] App unmount failed:', err);
      }

      try {
        el.innerHTML = '';

        if (document.documentElement.getAttribute('data-service') === 'drive') {
          document.documentElement.removeAttribute('data-service');
        }

        console.log('[Drive] Cleanup completed');
      } catch (err) {
        console.error('[Drive] Cleanup failed:', err);
      }

      console.groupEnd();
    }
  };
}
