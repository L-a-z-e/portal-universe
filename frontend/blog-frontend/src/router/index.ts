import {createMemoryHistory, createRouter, createWebHistory, type Router, type RouteRecordRaw} from "vue-router";

import PostListPage from '../views/PostListPage.vue';
import PostDetailPage from '../views/PostDetailPage.vue';
import PostWritePage from '../views/PostWritePage.vue';
import PostEditPage from '../views/PostEditPage.vue';

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
 * Embedded 모드용 Router (Module Federation으로 사용될 때)
 * - Memory History 사용
 * - Parent(Portal Shell)가 URL을 관리
 */
export function createBlogRouter(basePath: string = '/'): Router {
  console.log(`🔧 [Blog Router] Creating router for EMBEDDED mode`);
  console.log(`   Base path: ${basePath}`);

  const router = createRouter({
    history: createMemoryHistory(basePath),
    routes
  });

  // ✅ 초기 경로 설정
  router.push('/').catch(err => {
    console.error('❌ [Blog Router] Initial navigation failed:', err);
  });

  console.log('✅ [Blog Router] Router created (Memory History)');
  return router;
}

/**
 * Standalone 모드용 Router (직접 접속할 때)
 * - Web History 사용
 * - 브라우저 URL을 직접 관리
 */
export function createStandaloneBlogRouter(): Router {
  console.log(`🔧 [Blog Router] Creating router for STANDALONE mode`);

  const router = createRouter({
    history: createWebHistory('/'),
    routes
  });

  console.log('✅ [Blog Router] Router created (Web History)');
  console.log(`   Current path: ${router.currentRoute.value.path}`);

  return router;
}

/**
 * Router 설정 요약 출력 (디버깅용)
 */
export function logRouterInfo(router: Router) {
  console.log('📋 [Blog Router] Configuration:');
  console.log('   Routes:', routes.map(r => r.path).join(', '));
  console.log('   Current route:', router.currentRoute.value.path);
  console.log('   History type:', router.options.history.constructor.name);
}