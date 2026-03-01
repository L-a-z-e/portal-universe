// blog-frontend/src/bootstrap.ts
import './style.css';
import { createApp, type App as VueApp } from 'vue';
import App from './App.vue';
import type { Router } from 'vue-router';
import { createBlogRouter, logRouterInfo } from "./router";
import {createPinia} from "pinia";
import {useFollowStore} from "./stores/followStore";
import { disposePortalAuth } from '@portal/vue-bridge';

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
  /** 🆕 keep-alive activated 콜백 */
  onActivated?: () => void;
  /** 🆕 keep-alive deactivated 콜백 */
  onDeactivated?: () => void;
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

  // ✅ Portal Shell에서 마운트됨을 표시 (isEmbedded 플래그 활성화)
  (window as any).__POWERED_BY_PORTAL_SHELL__ = true;

  // ✅ 필수 파라미터 검증
  if (!el) {
    console.error('❌ [Blog] Mount element is null!');
    console.groupEnd();
    throw new Error('[Blog] Mount element is required');
  }


  const { initialPath, onNavigate } = options;

  // Vue 앱 생성
  const app: VueApp = createApp(App);

  const pinia = createPinia();
  app.use(pinia);

  // Router 생성 (Memory History)
  const router = createBlogRouter('/');
  app.use(router);

  // 디버깅 정보 출력
  logRouterInfo(router);

  // ✅ 초기 경로 설정
  const targetPath = initialPath || '/';

  router.push(targetPath).catch(err => {
    console.error('❌ [Blog] Initial navigation failed:', err);
  });

  // ✅ Parent에게 경로 변경 알림
  router.afterEach((to, from) => {
    if (to.path !== from.path) {
      onNavigate?.(to.path);
    }
  });

  // DOM에 마운트
  app.mount(el);

  // ✅ 로그아웃 시 followStore 초기화
  const authChangedHandler = () => {
    try {
      const followStore = useFollowStore();
      followStore.reset();
    } catch { /* pinia not ready */ }
  };
  window.addEventListener('portal:auth-changed', authChangedHandler);

  console.groupEnd();

  // ✅ 앱 인스턴스 반환
  return {
    router,

    /**
     * Parent(Portal Shell)로부터 경로 변경 수신
     */
    onParentNavigate: (path: string) => {

      if (router.currentRoute.value.path !== path) {
        router.push(path).catch(err => {
          console.error('❌ [Blog] Parent navigation failed:', err);
        });
      } else {
      }
    },

    /**
     * 🆕 keep-alive activated 콜백
     * RemoteWrapper의 onActivated에서 호출됨
     * Shopping → Blog 전환 시 data-service="shopping"이 유지되는 문제 해결
     */
    onActivated: () => {
      document.documentElement.setAttribute('data-service', 'blog');
    },

    /**
     * 🆕 keep-alive deactivated 콜백
     * RemoteWrapper의 onDeactivated에서 호출됨
     */
    onDeactivated: () => {
    },

    /**
     * 앱 언마운트 및 클린업
     *
     * CSS lifecycle은 Portal Shell(RemoteWrapper)에서 중앙 관리
     * Remote app은 Vue app unmount와 DOM 정리만 담당
     */
    unmount: () => {
      console.group('🔄 [Blog] Unmounting app');

      // 0. Portal auth 구독 해제 + 이벤트 리스너 정리
      disposePortalAuth();
      window.removeEventListener('portal:auth-changed', authChangedHandler);

      // 1. Vue App Unmount
      try {
        app.unmount();
      } catch (err) {
        console.error('❌ [Blog] App unmount failed:', err);
      }

      // 2. DOM Cleanup (CSS는 Portal Shell에서 관리)
      try {
        el.innerHTML = '';

        if (document.documentElement.getAttribute('data-service') === 'blog') {
          document.documentElement.removeAttribute('data-service');
        }

      } catch (err) {
        console.error('❌ [Blog] Cleanup failed:', err);
      }

      console.groupEnd();
    }
  };
}