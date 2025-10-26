import { createApp, type App as VueApp } from 'vue';
import App from './App.vue';
import type { Router } from 'vue-router';
import { createBlogRouter, logRouterInfo } from "./router";

/**
 * @file bootstrap.ts
 * @description 이 파일은 Module Federation을 통해 Portal Shell에 노출되는 진입점입니다.
 * `mountBlogApp` 함수를 내보내어, 셸이 이 Blog 앱을 동적으로 마운트하고 제어할 수 있도록 합니다.
 */

/**
 * `mountBlogApp` 함수에 전달될 옵션 타입입니다.
 */
export type MountOptions = {
  /** Remote 앱이 시작될 초기 경로 (예: '/write', '/post/123') */
  initialPath?: string;
  /** Remote 앱 내부에서 경로 변경이 발생했을 때 셸에 알리기 위한 콜백 함수 */
  onNavigate?: (path: string) => void;
}

/**
 * 마운트된 Blog 앱의 인스턴스 타입입니다.
 * 셸이 마운트된 앱을 제어할 수 있는 핸들러들을 포함합니다.
 */
export type BlogAppInstance = {
  /** 마운트된 앱의 Vue Router 인스턴스 */
  router: Router;
  /** 셸의 경로 변경을 Remote 앱에 전파하기 위한 함수 */
  onParentNavigate: (path: string) => void;
  /** 앱을 언마운트하고 리소스를 정리하는 함수 */
  unmount: () => void;
}

/**
 * Blog 앱을 지정된 DOM 엘리먼트에 마운트합니다. (Embedded 모드 전용)
 *
 * @param el - 앱을 마운트할 HTML 엘리먼트
 * @param options - 마운트 옵션 (초기 경로, 내비게이션 콜백 등)
 * @returns {BlogAppInstance} 셸이 앱을 제어할 수 있는 인스턴스 객체
 */
export function mountBlogApp(
  el: HTMLElement,
  options: MountOptions = {}
): BlogAppInstance {
  console.group('🚀 [Blog] Mounting app in EMBEDDED mode');

  if (!el) {
    console.error('❌ [Blog] Mount element is null!');
    console.groupEnd();
    throw new Error('[Blog] Mount element is required');
  }

  const { initialPath, onNavigate } = options;

  // 1. Vue 앱 인스턴스 생성
  const app: VueApp = createApp(App);

  // 2. Embedded 모드에 맞는 Memory History 기반의 라우터 생성
  const router = createBlogRouter('/');
  app.use(router);

  // 3. 초기 경로로 이동
  const targetPath = initialPath || '/';
  router.push(targetPath).catch(err => {
    console.error(`❌ [Blog] Initial navigation to '${targetPath}' failed:`, err);
  });

  // 4. 경로 변경 시 onNavigate 콜백을 호출하여 셸에 알림
  router.afterEach((to, from) => {
    if (to.path !== from.path) {
      console.log(`📍 [Blog] Route changed: ${from.path} → ${to.path}. Notifying shell.`);
      onNavigate?.(to.path);
    }
  });

  // 5. DOM에 앱 마운트
  app.mount(el);
  console.log('✅ [Blog] App mounted successfully');
  console.groupEnd();

  // 6. 셸이 앱을 제어할 수 있도록 인스턴스 반환
  return {
    router,
    /**
     * 셸의 경로 변경을 수신하여 앱의 경로를 업데이트합니다.
     */
    onParentNavigate: (path: string) => {
      console.log(`📥 [Blog] Received navigation from parent: ${path}`);
      if (router.currentRoute.value.path !== path) {
        router.push(path).catch(err => {
          console.error(`❌ [Blog] Parent navigation to '${path}' failed:`, err);
        });
      }
    },
    /**
     * 앱을 언마운트하고 관련 리소스를 정리합니다.
     */
    unmount: () => {
      console.group('🔄 [Blog] Unmounting app');
      try {
        app.unmount();
        el.innerHTML = ''; // 컨테이너 비우기
        console.log('✅ [Blog] App unmounted successfully');
      } catch (err) {
        console.error('❌ [Blog] Unmount failed:', err);
      }
      console.groupEnd();
    }
  };
}
