import { createRouter, createWebHistory, type RouteLocationNormalized } from 'vue-router';
import RemoteWrapper from '../components/RemoteWrapper.vue';
import HomePage from "../views/HomePage.vue";
import CallbackPage from "../views/CallbackPage.vue";

/**
 * Blog Remote 동적 로더
 * - blog_remote가 없어도 portal-shell은 정상 동작
 */
async function loadBlogBootstrap() {
  try {
    const module = await import('blog_remote/bootstrap');
    return module.mountBlogApp;
  } catch (error) {
    console.warn('⚠️ Blog remote not available:', error);
    return null;
  }
}

const routes = [
  { path: '/', name: 'Home', component: HomePage },
  { path: '/callback', name: 'Callback', component: CallbackPage },
  {
    path: '/blog/:pathMatch(.*)*',
    name: 'blog',
    component: RemoteWrapper,
    props: async (route: RouteLocationNormalized) => {
      const mountFn = await loadBlogBootstrap();

      return {
        mountFn, // null일 수 있음
        basePath: '/blog',
        initialPath: route.path.substring('/blog'.length) || '/',
        remoteName: 'Blog' // 에러 표시용
      };
    }
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

// 전역 에러 핸들러
router.onError((error) => {
  console.error('❌ Router error:', error);
  // portal-shell은 계속 동작
});

export default router;