// blog-frontend/src/bootstrap.ts
import './style.css';
import { createApp, type App as VueApp } from 'vue';
import App from './App.vue';
import type { Router } from 'vue-router';
import { createBlogRouter, logRouterInfo } from "./router";
import {createPinia} from "pinia";

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

  const pinia = createPinia();
  app.use(pinia);

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
     * 🆕 keep-alive activated 콜백
     * RemoteWrapper의 onActivated에서 호출됨
     * Shopping → Blog 전환 시 data-service="shopping"이 유지되는 문제 해결
     */
    onActivated: () => {
      console.log('🔄 [Blog] App activated (keep-alive)');
      document.documentElement.setAttribute('data-service', 'blog');
      console.log('[Blog] KeepAlive activated: Restored data-service="blog"');
    },

    /**
     * 🆕 keep-alive deactivated 콜백
     * RemoteWrapper의 onDeactivated에서 호출됨
     */
    onDeactivated: () => {
      console.log('⏸️ [Blog] App deactivated (keep-alive)');
    },

    /**
     * 앱 언마운트 및 클린업
     * 
     * 🔴 핵심: <head>의 Blog CSS 스타일 태그 제거!
     * KeepAlive로 인해 언마운트 시에도 CSS가 남아있기 때문에
     * 수동으로 <head>에서 Blog CSS를 찾아서 제거해야 함
     */
    unmount: () => {
      console.group('🔄 [Blog] Unmounting app');

      // 1. Vue App Unmount
      try {
        app.unmount();
        console.log('✅ [Blog] App unmounted successfully');
      } catch (err) {
        console.error('❌ [Blog] App unmount failed:', err);
      }

      // 2. DOM & Style Cleanup (Always execute)
      try {
        el.innerHTML = '';

        // 🟢 Step 1: <head>의 모든 <style> 태그 중 Blog CSS 제거
        // CSS 번들된 파일명: blog-frontend.css 또는 style.css
        const styleTags = document.querySelectorAll('style');
        console.log(`🔍 [Blog] Found ${styleTags.length} <style> tags, searching for Blog CSS...`);
        
        styleTags.forEach((styleTag, index) => {
          const content = styleTag.textContent || '';
          
          // Blog 관련 CSS 마커 확인
          // [data-service="blog"] 또는 기타 Blog 특정 스타일이 있으면 제거
          if (content.includes('[data-service="blog"]') ||
              content.includes('blog-') ||
              (content.includes('@import') && content.includes('blog'))) {
            console.log(`   📍 [Blog] Found Blog CSS at index ${index}, removing...`);
            styleTag.remove();
          }
        });
        
        // 🟢 Step 2: <link> 태그 중 Blog CSS 제거 (있는 경우)
        // Vite dev mode에서는 CSS가 localhost:30001에서 로드됨
        const linkTags = document.querySelectorAll('link[rel="stylesheet"]');
        linkTags.forEach((linkTag) => {
          const href = linkTag.getAttribute('href') || '';
          // Blog CSS 식별: origin이 30001 포트이거나 data-mf-app="blog" 마커가 있는 경우
          const isBlogCss = href.includes('localhost:30001') ||
                           href.includes(':30001/') ||
                           linkTag.hasAttribute('data-mf-app') && linkTag.getAttribute('data-mf-app') === 'blog';
          if (isBlogCss) {
            console.log(`   📍 [Blog] Found Blog CSS link: ${href}, removing...`);
            linkTag.remove();
          }
        });
        
        // 🟢 Step 3: data-service 속성 정리
        if (document.documentElement.getAttribute('data-service') === 'blog') {
          console.log('   📍 [Blog] Resetting data-service attribute...');
          // Portal로 복귀 시 Portal App.vue에서 다시 설정되므로 여기선 비우기만 함
          document.documentElement.removeAttribute('data-service');
        }
        
        console.log('✅ [Blog] Cleanup completed - CSS removed from <head>');
      } catch (err) {
        console.error('❌ [Blog] Cleanup failed:', err);
      }

      console.groupEnd();
    }
  };
}