import {createMemoryHistory, createRouter, createWebHistory, type Router, type RouteRecordRaw} from "vue-router";

import PostListPage from '../views/PostListPage.vue';
import PostDetailPage from '../views/PostDetailPage.vue';
import PostWritePage from '../views/PostWritePage.vue';
import PostEditPage from '../views/PostEditPage.vue';

/**
 * @file router/index.ts
 * @description Blog 앱의 라우팅을 설정합니다.
 * 실행 모드(Standalone/Embedded)에 따라 다른 History 방식의 라우터를 생성하는 팩토리 함수를 제공합니다.
 */

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'PostList',
    component: PostListPage
  },
  {
    path: '/:postId',
    name: 'PostDetail',
    component: PostDetailPage,
    props: true
  },
  {
    path: '/write',
    name: 'PostWrite',
    component: PostWritePage
  },
  {
    path: '/edit/:postId',
    name: 'PostEdit',
    component: PostEditPage,
    props: true
  }
];

/**
 * Embedded 모드(셸에 포함될 때)를 위한 라우터를 생성합니다.
 * 브라우저의 주소 표시줄을 직접 제어하지 않는 `createMemoryHistory`를 사용합니다.
 * 모든 라우팅 제어권은 상위 셸(Portal Shell)에 위임됩니다.
 *
 * @param basePath 라우터의 기본 경로 (일반적으로 '/')
 * @returns {Router} Memory History를 사용하는 Vue Router 인스턴스
 */
export function createBlogRouter(basePath: string = '/'): Router {
  console.log(`🔧 [Blog Router] Creating router for EMBEDDED mode`);

  const router = createRouter({
    history: createMemoryHistory(basePath),
    routes
  });

  return router;
}

/**
 * Standalone 모드(단독 실행될 때)를 위한 라우터를 생성합니다.
 * 브라우저의 주소 표시줄과 상호작용하는 `createWebHistory`를 사용합니다.
 *
 * @returns {Router} Web History를 사용하는 Vue Router 인스턴스
 */
export function createStandaloneBlogRouter(): Router {
  console.log(`🔧 [Blog Router] Creating router for STANDALONE mode`);

  const router = createRouter({
    history: createWebHistory('/'),
    routes
  });

  return router;
}

/**
 * 생성된 라우터의 설정을 콘솔에 출력하는 디버깅용 유틸리티 함수입니다.
 * @param router 정보를 출력할 라우터 인스턴스
 */
export function logRouterInfo(router: Router) {
  console.log('📋 [Blog Router] Configuration:');
  console.log('   Routes:', routes.map(r => r.path).join(', '));
  console.log('   Current route:', router.currentRoute.value.path);
  console.log('   History type:', router.options.history.constructor.name);
}
