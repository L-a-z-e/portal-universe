// portal-shell/src/router/index.ts

import { createRouter, createWebHistory } from 'vue-router';
import RemoteWrapper from '../components/RemoteWrapper.vue';
import HomePage from "../views/HomePage.vue";
import SignupPage from "../views/SignupPage.vue";
import CallbackPage from "../views/CallbackPage.vue";
import { getRemoteConfigs } from '../config/remoteRegistry';

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
    path: '/signup',
    name: 'Signup',
    component: SignupPage,
    meta: { title: '회원가입' }
  },
  {
    path: '/callback',
    name: 'Callback',
    component: CallbackPage
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

router.onError((error) => {
  console.error('❌ Router error:', error);
  // Portal Shell은 계속 동작
});

export default router;