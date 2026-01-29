// portal-shell/src/router/index.ts

import { createRouter, createWebHistory } from 'vue-router';
import type { RouteLocationNormalized } from 'vue-router';
import RemoteWrapper from '../components/RemoteWrapper.vue';
import HomePage from "../views/HomePage.vue";
import DashboardPage from "../views/DashboardPage.vue";
import SignupPage from "../views/SignupPage.vue";
import CallbackPage from "../views/CallbackPage.vue";
import OAuth2Callback from "../views/OAuth2Callback.vue";
import SettingsPage from "../views/SettingsPage.vue";
import ServiceStatusPage from "../views/ServiceStatusPage.vue";
import MyProfilePage from "../views/MyProfilePage.vue";
import ForbiddenPage from "../views/ForbiddenPage.vue";
import { getRemoteConfigs } from '../config/remoteRegistry';
import { useAuthStore } from '../store/auth';

declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth?: boolean;
    requiresRoles?: string[];
    title?: string;
    remoteName?: string;
    icon?: string;
    keepAlive?: boolean;
  }
}

function createRemoteRoutes() {
  const configs = getRemoteConfigs();

  return configs.map(config => ({
    path: `${config.basePath}/:pathMatch(.*)*`,
    name: config.key,
    component: RemoteWrapper,
    props: (route: any) => ({
      config,  // RemoteConfig 객체 전달
      initialPath: route.path.substring(config.basePath.length) || '/'
    }),
    meta: {
      remoteName: config.key,
      icon: config.icon,
      keepAlive: true
    }
  }));
}

const routes = [
  {
    path: '/',
    name: 'Home',
    component: HomePage,
    meta: { title: '홈' }
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: DashboardPage,
    meta: { title: '대시보드', requiresAuth: true }
  },
  {
    path: '/signup',
    name: 'Signup',
    component: SignupPage,
    meta: { title: '회원가입' }
  },
  {
    path: '/callback',
    name: 'Callback',
    component: CallbackPage,
    meta: { title: '로그인 처리 중' }
  },
  {
    path: '/oauth2/callback',
    name: 'OAuth2Callback',
    component: OAuth2Callback,
    meta: { title: 'OAuth2 로그인 처리 중', requiresAuth: false }
  },
  {
    path: '/settings',
    name: 'Settings',
    component: SettingsPage,
    meta: { title: '설정' }
  },
  {
    path: '/status',
    name: 'ServiceStatus',
    component: ServiceStatusPage,
    meta: { title: '서비스 상태' }
  },
  {
    path: '/profile',
    name: 'MyProfile',
    component: MyProfilePage,
    meta: { title: '내 프로필', requiresAuth: true }
  },

  // 403 권한 부족 페이지
  {
    path: '/403',
    name: 'Forbidden',
    component: ForbiddenPage,
    meta: { title: '접근 권한 없음' }
  },

  // ✅ Remote 라우트 동적 생성
  ...createRemoteRoutes(),

  // 404 페이지
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('../views/NotFound.vue')
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

// Navigation Guard: 인증 및 권한 체크
router.beforeEach((to: RouteLocationNormalized, _from: RouteLocationNormalized) => {
  const authStore = useAuthStore();

  // 인증 필요 라우트 체크
  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    console.log(`[Router Guard] Auth required for ${to.path}, showing login modal`);
    authStore.requestLogin();
    return false; // 이동 차단
  }

  // 역할 기반 접근 제어
  if (to.meta.requiresRoles && to.meta.requiresRoles.length > 0) {
    if (!authStore.isAuthenticated) {
      authStore.requestLogin();
      return false;
    }

    if (!authStore.hasAnyRole(to.meta.requiresRoles)) {
      console.log(`[Router Guard] Insufficient roles for ${to.path}`);
      return { path: '/403' };
    }
  }

  return true;
});

router.onError((error) => {
  console.error('❌ Router error:', error);
  // Portal Shell은 계속 동작
});

export default router;