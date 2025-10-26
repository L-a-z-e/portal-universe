import { createRouter, createWebHistory } from 'vue-router';
import RemoteWrapper from '../components/RemoteWrapper.vue';
import HomePage from "../views/HomePage.vue";
import CallbackPage from "../views/CallbackPage.vue";
import { getRemoteConfigs } from '../config/remoteRegistry';
import type { RemoteConfig } from "../config/remoteRegistry";

/**
 * remoteRegistry에 등록된 설정들을 기반으로
 * 각 마이크로 프론트엔드(Remote)에 대한 라우트들을 동적으로 생성합니다.
 * @returns {any[]} 생성된 라우트 객체 배열
 */
function createRemoteRoutes() {
  const configs: RemoteConfig[] = getRemoteConfigs();

  return configs.map(config => ({
    // 예: '/blog'로 시작하는 모든 경로를 이 라우트가 처리합니다. '/blog/post/123' 등
    path: `${config.basePath}/:pathMatch(.*)*`,
    name: config.key,
    component: RemoteWrapper, // 모든 Remote 라우트는 RemoteWrapper 컴포넌트를 통해 렌더링됩니다.
    props: (route: any) => ({
      config,  // 현재 라우트에 해당하는 RemoteConfig 객체를 prop으로 전달합니다.
      // 셸의 전체 경로에서 Remote의 기본 경로를 제외한 나머지 부분을 initialPath로 전달합니다.
      // 예: /blog/post/123 -> /post/123
      initialPath: route.path.substring(config.basePath.length) || '/'
    }),
    meta: {
      remoteName: config.name,
      icon: config.icon
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
    path: '/callback',
    name: 'Callback',
    component: CallbackPage, // OIDC 로그인 콜백 처리를 위한 페이지
    meta: { title: '로그인 중...' }
  },

  // 동적으로 생성된 Remote 라우트들을 여기에 펼쳐 넣습니다.
  ...createRemoteRoutes(),

  // 일치하는 라우트가 없을 경우 404 페이지를 보여줍니다.
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

/**
 * Vue Router에서 발생하는 전역 에러를 처리합니다.
 * 주로 비동기 컴포넌트 로딩 실패 시 발생할 수 있습니다.
 */
router.onError((error) => {
  console.error('❌ Router error:', error);
  // 라우팅 에러가 발생하더라도 셸 앱 자체는 중단되지 않도록 합니다.
});

export default router;
