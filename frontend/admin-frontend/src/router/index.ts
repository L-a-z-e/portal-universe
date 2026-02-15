// admin-frontend/src/router/index.ts
import { createMemoryHistory, createRouter, createWebHistory, type Router, type RouteRecordRaw } from 'vue-router';
import { getPortalAuthState } from '@portal/vue-bridge';

import AdminLayout from '../layouts/AdminLayout.vue';
import DashboardPage from '../views/DashboardPage.vue';
import UsersPage from '../views/UsersPage.vue';
import RolesPage from '../views/RolesPage.vue';
import MembershipsPage from '../views/MembershipsPage.vue';
import SellerApprovalsPage from '../views/SellerApprovalsPage.vue';
import AuditLogPage from '../views/AuditLogPage.vue';

function addAuthGuard(router: Router): void {
  router.beforeEach((to) => {
    if (!to.meta.requiresAuth) return true;

    // authAdapter 기반 인증 확인 (Embedded + Standalone 공통)
    const authState = getPortalAuthState();
    if (authState.isAuthenticated) return true;

    // Standalone fallback: 글로벌 토큰으로 확인
    if ((window as any).__PORTAL_ACCESS_TOKEN__) return true;

    console.log(`[Admin Router Guard] Auth required for ${to.path}`);
    if (typeof (window as any).__PORTAL_SHOW_LOGIN__ === 'function') {
      (window as any).__PORTAL_SHOW_LOGIN__();
    }
    return false;
  });
}

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: AdminLayout,
    meta: { requiresAuth: true },
    children: [
      { path: '', name: 'Dashboard', component: DashboardPage },
      { path: 'users', name: 'Users', component: UsersPage },
      { path: 'roles', name: 'Roles', component: RolesPage },
      { path: 'memberships', name: 'Memberships', component: MembershipsPage },
      { path: 'seller-approvals', name: 'SellerApprovals', component: SellerApprovalsPage },
      { path: 'audit-log', name: 'AuditLog', component: AuditLogPage },
    ]
  }
];

export function createAdminRouter(basePath: string = '/'): Router {
  const router = createRouter({
    history: createMemoryHistory(basePath),
    routes
  });
  addAuthGuard(router);
  router.push('/').catch(() => {});
  return router;
}

export function createStandaloneAdminRouter(): Router {
  const router = createRouter({
    history: createWebHistory('/'),
    routes
  });
  addAuthGuard(router);
  return router;
}
