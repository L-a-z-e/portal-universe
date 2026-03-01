// blog-frontend/src/router/index.ts

import {createMemoryHistory, createRouter, createWebHistory, type Router, type RouteRecordRaw} from "vue-router";

const PostListPage = () => import('../views/PostListPage.vue');
const PostDetailPage = () => import('../views/PostDetailPage.vue');
const PostWritePage = () => import('../views/PostWritePage.vue');
const PostEditPage = () => import('../views/PostEditPage.vue');
const SeriesDetailPage = () => import('../views/SeriesDetailPage.vue');
const TagListPage = () => import('../views/TagListPage.vue');
const TagDetailPage = () => import('../views/TagDetailPage.vue');
const UserBlogPage = () => import('../views/UserBlogPage.vue');
const MyPage = () => import('../views/MyPage.vue');
const CategoryListPage = () => import('../views/CategoryListPage.vue');
const AdvancedSearchPage = () => import('../views/AdvancedSearchPage.vue');
const StatsPage = () => import('../views/StatsPage.vue');
const SeriesListPage = () => import('../views/SeriesListPage.vue');

import { getPortalAuthState } from '@portal/vue-bridge';

/**
 * Navigation guard: requiresAuth 체크
 * Embedded 모드에서는 usePortalAuth(authAdapter 기반)를 사용하고,
 * Standalone 모드에서는 window.__PORTAL_ACCESS_TOKEN__ 존재 여부로 판단
 */
function addAuthGuard(router: Router): void {
  router.beforeEach((to, _from) => {
    if (!to.meta.requiresAuth) return true;

    // authAdapter 기반 인증 확인 (Embedded + Standalone 공통)
    const authState = getPortalAuthState();
    if (authState.isAuthenticated) return true;

    // Standalone fallback: 글로벌 토큰으로 확인
    if ((window as any).__PORTAL_ACCESS_TOKEN__) return true;

    // 미인증 → Portal Shell에 로그인 모달 요청
    if (typeof (window as any).__PORTAL_SHOW_LOGIN__ === 'function') {
      (window as any).__PORTAL_SHOW_LOGIN__();
    }
    return false;
  });
}

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'PostList',
    component: PostListPage
  },
  {
    path: '/tags',
    name: 'TagList',
    component: TagListPage
  },
  {
    path: '/tags/:tagName',
    name: 'TagDetail',
    component: TagDetailPage,
    props: true
  },
  {
    path: '/categories',
    name: 'CategoryList',
    component: CategoryListPage
  },
  {
    path: '/search/advanced',
    name: 'AdvancedSearch',
    component: AdvancedSearchPage
  },
  {
    path: '/stats',
    name: 'Stats',
    component: StatsPage
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
  },
  {
    path: '/series',
    name: 'SeriesList',
    component: SeriesListPage
  },
  {
    path: '/series/:seriesId',
    name: 'SeriesDetail',
    component: SeriesDetailPage,
    props: true
  },
  {
    path: '/my',
    name: 'MyPage',
    component: MyPage,
    meta: { requiresAuth: true }
  },
  {
    path: '/@:username',
    name: 'UserBlog',
    component: UserBlogPage,
    props: true
  },
  {
    path: '/:postId',
    name: 'PostDetail',
    component: PostDetailPage,
    props: true
  }
];

/**
 * Embedded 모드용 Router (Module Federation으로 사용될 때)
 * - Memory History 사용
 * - Parent(Portal Shell)가 URL을 관리
 */
export function createBlogRouter(basePath: string = '/'): Router {

  const router = createRouter({
    history: createMemoryHistory(basePath),
    routes
  });

  addAuthGuard(router);

  // ✅ 초기 경로 설정
  router.push('/').catch(err => {
    console.error('❌ [Blog Router] Initial navigation failed:', err);
  });

  return router;
}

/**
 * Standalone 모드용 Router (직접 접속할 때)
 * - Web History 사용
 * - 브라우저 URL을 직접 관리
 */
export function createStandaloneBlogRouter(): Router {

  const router = createRouter({
    history: createWebHistory('/'),
    routes
  });

  addAuthGuard(router);


  return router;
}

/**
 * Router 설정 요약 출력 (디버깅용)
 */
export function logRouterInfo(router: Router) {
}