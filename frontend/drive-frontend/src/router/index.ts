import { createMemoryHistory, createRouter, createWebHistory, type Router, type RouteRecordRaw } from "vue-router";
import { getPortalAuthState } from '@portal/vue-bridge';

import DrivePage from '../views/DrivePage.vue';

function addAuthGuard(router: Router): void {
  router.beforeEach((to, _from) => {
    if (!to.meta.requiresAuth) return true;

    // authAdapter 기반 인증 확인 (Embedded + Standalone 공통)
    const authState = getPortalAuthState();
    if (authState.isAuthenticated) return true;

    // Standalone fallback: 글로벌 토큰으로 확인
    if ((window as any).__PORTAL_ACCESS_TOKEN__) return true;

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

  const router = createRouter({
    history: createMemoryHistory(basePath),
    routes
  });

  addAuthGuard(router);

  router.push('/').catch(err => {
    console.error('[Drive Router] Initial navigation failed:', err);
  });

  return router;
}

export function createStandaloneDriveRouter(): Router {

  const router = createRouter({
    history: createWebHistory('/'),
    routes
  });

  addAuthGuard(router);


  return router;
}

export function logRouterInfo(router: Router) {
}
