// blog-frontend/src/bootstrap.ts
import { createApp, type App as VueApp } from 'vue';
import App from './App.vue';
import type { Router } from 'vue-router';
import { createBlogRouter, logRouterInfo } from "./router";

/**
 * Mount 옵션
 */
export type MountOptions = {
  /** 초기 경로 (예: '/write', '/123') */
  initialPath?: string;
  /** Parent에게 경로 변경 알림 */
  onNavigate?: (path: string) => void;
}

/**
 * Mount된 Blog 앱 인스턴스
 */
export type BlogAppInstance = {
  /** Vue Router 인스턴스 */
  router: Router;
  /** Parent로부터 경로 변경 수신 */
  onParentNavigate: (path: string) => void;
  /** 앱 언마운트 */
  unmount: () => void;
}

/**
 * Blog 앱을 지정된 컨테이너에 마운트 (Embedded 모드)
 *
 * @param el - 마운트할 HTML 엘리먼트
 * @param options - 마운트 옵션
 * @returns Blog 앱 인스턴스 (router, onParentNavigate, unmount)
 *
 * @example
 * ```
 * const blogApp = mountBlogApp(container, {
 *   initialPath: '/123',
 *   onNavigate: (path) => console.log('Navigated to:', path)
 * });
 * ```
 */
export function mountBlogApp(
  el: HTMLElement,
  options: MountOptions = {}
): BlogAppInstance {
  console.group('🚀 [Blog] Mounting app in EMBEDDED mode');

  // ✅ 필수 파라미터 검증
  if (!el) {
    console.error('❌ [Blog] Mount element is null!');
    console.groupEnd();
    throw new Error('[Blog] Mount element is required');
  }

  console.log('📍 Mount target:', el.tagName, el.className || '(no class)');
  console.log('📍 Options:', options);

  const { initialPath, onNavigate } = options;

  // Vue 앱 생성
  const app: VueApp = createApp(App);

  // Router 생성 (Memory History)
  const router = createBlogRouter('/');
  app.use(router);

  // 디버깅 정보 출력
  logRouterInfo(router);

  // ✅ 초기 경로 설정
  const targetPath = initialPath || '/';
  console.log(`🔄 [Blog] Navigating to: ${targetPath}`);

  router.push(targetPath).catch(err => {
    console.error('❌ [Blog] Initial navigation failed:', err);
  });

  // ✅ Parent에게 경로 변경 알림
  router.afterEach((to, from) => {
    if (to.path !== from.path) {
      console.log(`📍 [Blog] Route changed: ${from.path} → ${to.path}`);
      onNavigate?.(to.path);
    }
  });

  // DOM에 마운트
  app.mount(el);
  console.log('✅ [Blog] App mounted successfully');
  console.groupEnd();

  // ✅ 앱 인스턴스 반환
  return {
    router,

    /**
     * Parent(Portal Shell)로부터 경로 변경 수신
     */
    onParentNavigate: (path: string) => {
      console.log(`📥 [Blog] Received navigation from parent: ${path}`);

      if (router.currentRoute.value.path !== path) {
        router.push(path).catch(err => {
          console.error('❌ [Blog] Parent navigation failed:', err);
        });
      } else {
        console.log('   ℹ️ Already on this path, skipping navigation');
      }
    },

    /**
     * 앱 언마운트 및 클린업
     */
    unmount: () => {
      console.group('🔄 [Blog] Unmounting app');

      try {
        app.unmount();
        el.innerHTML = '';
        console.log('✅ [Blog] App unmounted successfully');
      } catch (err) {
        console.error('❌ [Blog] Unmount failed:', err);
      }

      console.groupEnd();
    }
  };
}