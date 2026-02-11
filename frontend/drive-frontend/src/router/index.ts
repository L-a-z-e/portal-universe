import { createMemoryHistory, createRouter, createWebHistory, type Router, type RouteRecordRaw } from "vue-router";

import DrivePage from '../views/DrivePage.vue';

function addAuthGuard(router: Router): void {
  router.beforeEach(async (to, _from) => {
    if (!to.meta.requiresAuth) return true;

    try {
      const { useAuthStore } = await import('portal/stores');
      const authStore = useAuthStore();
      if (authStore.isAuthenticated.value) return true;
    } catch {
      if ((window as any).__PORTAL_ACCESS_TOKEN__) return true;
    }

    console.log(`[Drive Router Guard] Auth required for ${to.path}`);
    if (typeof (window as any).__PORTAL_SHOW_LOGIN__ === 'function') {
      (window as any).__PORTAL_SHOW_LOGIN__();
    }
    return false;
  });
}

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'DriveHome',
    component: DrivePage
  }
];

export function createDriveRouter(basePath: string = '/'): Router {
  console.log(`[Drive Router] Creating router for EMBEDDED mode`);
  console.log(`   Base path: ${basePath}`);

  const router = createRouter({
    history: createMemoryHistory(basePath),
    routes
  });

  addAuthGuard(router);

  router.push('/').catch(err => {
    console.error('[Drive Router] Initial navigation failed:', err);
  });

  console.log('[Drive Router] Router created (Memory History)');
  return router;
}

export function createStandaloneDriveRouter(): Router {
  console.log(`[Drive Router] Creating router for STANDALONE mode`);

  const router = createRouter({
    history: createWebHistory('/'),
    routes
  });

  addAuthGuard(router);

  console.log('[Drive Router] Router created (Web History)');
  console.log(`   Current path: ${router.currentRoute.value.path}`);

  return router;
}

export function logRouterInfo(router: Router) {
  console.log('[Drive Router] Configuration:');
  console.log('   Routes:', routes.map(r => r.path).join(', '));
  console.log('   Current route:', router.currentRoute.value.path);
  console.log('   History type:', router.options.history.constructor.name);
}
