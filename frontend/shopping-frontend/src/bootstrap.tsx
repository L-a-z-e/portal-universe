/// <reference types="vite/client" />
import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App'
import { navigateTo } from './router'
import './styles/index.css'

/**
 * Mount 옵션 (Blog와 동일한 인터페이스)
 */
export type MountOptions = {
  /** 초기 경로 (예: '/cart', '/orders') */
  initialPath?: string
  /** Parent에게 경로 변경 알림 */
  onNavigate?: (path: string) => void
}

/**
 * Mount된 Shopping 앱 인스턴스 (Blog와 동일한 인터페이스)
 */
export type ShoppingAppInstance = {
  /** Parent로부터 경로 변경 수신 */
  onParentNavigate: (path: string) => void
  /** 앱 언마운트 */
  unmount: () => void
}

// 앱 인스턴스 관리
let root: ReactDOM.Root | null = null
let currentProps: Record<string, any> = {}
let navigateCallback: ((path: string) => void) | null = null

/**
 * Shopping 앱을 지정된 컨테이너에 마운트 (Embedded 모드)
 * Blog와 동일한 인터페이스를 사용
 *
 * @param el - 마운트할 HTML 엘리먼트
 * @param options - 마운트 옵션
 * @returns Shopping 앱 인스턴스 (onParentNavigate, unmount)
 *
 * @example
 * ```
 * const shoppingApp = mountShoppingApp(container, {
 *   initialPath: '/cart',
 *   onNavigate: (path) => console.log('Navigated to:', path)
 * });
 * ```
 */
export function mountShoppingApp(
  el: HTMLElement,
  options: MountOptions = {}
): ShoppingAppInstance {
  console.group('🚀 [Shopping] Mounting app in EMBEDDED mode');

  // ✅ 필수 파라미터 검증 (Blog의 패턴 따름)
  if (!el) {
    console.error('❌ [Shopping] Mount element is null!');
    console.groupEnd();
    throw new Error('[Shopping] Mount element is required');
  }

  console.log('📍 Mount target:', el.tagName, el.className || '(no class)');

  const { initialPath = '/', onNavigate } = options;
  console.log('📍 Initial path:', initialPath);
  console.log('📍 Options:', { onNavigate: !!onNavigate });

  // 내비게이션 콜백 저장 (App에서 사용)
  navigateCallback = onNavigate || null;

  try {
    // ✅ Step 1: React 루트 생성
    root = ReactDOM.createRoot(el);
    currentProps = {
      initialPath,
      onNavigate: (path: string) => {
        console.log(`📍 [Shopping] Route changed to: ${path}`);
        navigateCallback?.(path);
      }
    };

    // ✅ Step 2: data-service="shopping" 속성 설정 (CSS 선택자 활성화)
    document.documentElement.setAttribute('data-service', 'shopping');
    console.log('[Shopping] Set data-service="shopping"');

    // ✅ Step 3: 초기 Props로 렌더링
    root.render(
      <React.StrictMode>
        <App {...currentProps} />
      </React.StrictMode>
    );
    console.log('✅ [Shopping] App mounted successfully');
    console.groupEnd();

    // ✅ Step 4: 앱 인스턴스 반환 (Blog와 동일한 인터페이스)
    return {
      /**
       * Parent(Portal Shell)로부터 경로 변경 수신
       * Blog의 onParentNavigate와 동일한 역할
       */
      onParentNavigate: (path: string) => {
        console.log(`📥 [Shopping] Received navigation from parent: ${path}`);
        // Router의 navigate 함수를 직접 호출하여 경로 변경
        navigateTo(path);
      },

      /**
       * 앱 언마운트 및 클린업
       * Blog의 unmount와 동일한 역할
       *
       * 🔴 핵심: <head>의 Shopping CSS 스타일 태그 제거!
       */
      unmount: () => {
        console.group('🔄 [Shopping] Unmounting app');

        // 1. React Root Unmount
        try {
          if (root) {
            root.unmount();
            root = null;
          }
          console.log('✅ [Shopping] App unmounted successfully');
        } catch (err) {
          console.error('❌ [Shopping] App unmount failed:', err);
        }

        // 2. DOM & Style Cleanup (Always execute)
        try {
          el.innerHTML = '';

          // 🟢 Step 1: <head>의 모든 <style> 태그 중 Shopping CSS 제거
          const styleTags = document.querySelectorAll('style');
          console.log(`🔍 [Shopping] Found ${styleTags.length} <style> tags, searching for Shopping CSS...`);

          styleTags.forEach((styleTag, index) => {
            const content = styleTag.textContent || '';

            // Shopping 관련 CSS 마커 확인
            if (content.includes('[data-service="shopping"]') ||
              content.includes('shopping-') ||
              (content.includes('@import') && content.includes('shopping'))) {
              console.log(`   📍 [Shopping] Found Shopping CSS at index ${index}, removing...`);
              styleTag.remove();
            }
          });

          // 🟢 Step 2: <link> 태그 중 Shopping CSS 제거 (있는 경우)
          const linkTags = document.querySelectorAll('link[rel="stylesheet"]');
          linkTags.forEach((linkTag) => {
            const href = linkTag.getAttribute('href') || '';
            if (href.includes('shopping') || href.includes('shopping-frontend')) {
              console.log(`   📍 [Shopping] Found Shopping CSS link: ${href}, removing...`);
              linkTag.remove();
            }
          });

          // 🟢 Step 3: data-service 속성 정리
          if (document.documentElement.getAttribute('data-service') === 'shopping') {
            console.log('   📍 [Shopping] Resetting data-service attribute...');
            document.documentElement.removeAttribute('data-service');
          }

          // Props 초기화
          currentProps = {};
          navigateCallback = null;
          console.log('✅ [Shopping] Cleanup completed - CSS removed from <head>');
        } catch (err) {
          console.error('❌ [Shopping] Cleanup failed:', err);
        }

        console.groupEnd();
      }
    };
  } catch (error) {
    console.error('❌ [Shopping] Mount failed:', error);
    console.groupEnd();
    throw error;
  }
}

// 타입 정의 (TypeScript)
export interface MountAPI {
  onParentNavigate: (path: string) => void;
  unmount: () => void;
}

// 호환성을 위한 기본 export
export default { mountShoppingApp }
