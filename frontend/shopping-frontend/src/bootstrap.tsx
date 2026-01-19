/// <reference types="vite/client" />
import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App'
import { navigateTo, resetRouter, setAppActive } from './router'
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
 * Mount된 Shopping 앱 인스턴스 (확장된 인터페이스)
 */
export type ShoppingAppInstance = {
  /** Parent로부터 경로 변경 수신 */
  onParentNavigate: (path: string) => void
  /** 앱 언마운트 */
  unmount: () => void
  /** 🆕 keep-alive activated 콜백 */
  onActivated?: () => void
  /** 🆕 keep-alive deactivated 콜백 */
  onDeactivated?: () => void
}

// 🆕 WeakMap으로 인스턴스별 상태 관리 (전역 상태 제거)
const instanceRegistry = new WeakMap<HTMLElement, {
  root: ReactDOM.Root
  navigateCallback: ((path: string) => void) | null
  styleObserver: MutationObserver | null
  isActive: boolean
}>()

/**
 * Shopping 앱을 지정된 컨테이너에 마운트 (Embedded 모드)
 * Blog와 동일한 인터페이스를 사용
 *
 * @param el - 마운트할 HTML 엘리먼트
 * @param options - 마운트 옵션
 * @returns Shopping 앱 인스턴스 (onParentNavigate, unmount, onActivated, onDeactivated)
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

  // ✅ Portal Shell에서 마운트됨을 표시 (isEmbedded 플래그 활성화)
  (window as any).__POWERED_BY_PORTAL_SHELL__ = true;

  // ✅ 필수 파라미터 검증 (Blog의 패턴 따름)
  if (!el) {
    console.error('❌ [Shopping] Mount element is null!');
    console.groupEnd();
    throw new Error('[Shopping] Mount element is required');
  }

  // 🆕 기존 인스턴스가 있으면 정리
  const existingInstance = instanceRegistry.get(el);
  if (existingInstance) {
    console.log('⚠️ [Shopping] Cleaning up existing instance...');
    try {
      existingInstance.styleObserver?.disconnect();
      existingInstance.root.unmount();
    } catch (err) {
      console.warn('⚠️ [Shopping] Existing instance cleanup warning:', err);
    }
    instanceRegistry.delete(el);
  }

  console.log('📍 Mount target:', el.tagName, el.className || '(no class)');

  const { initialPath = '/', onNavigate } = options;
  console.log('📍 Initial path:', initialPath);
  console.log('📍 Options:', { onNavigate: !!onNavigate });

  try {
    // ✅ Step 1: React 루트 생성 (함수 스코프 내 관리)
    const root = ReactDOM.createRoot(el);
    let navigateCallback = onNavigate || null;

    // 🆕 스타일 태그 마킹을 위한 MutationObserver
    const styleObserver = new MutationObserver((mutations) => {
      mutations.forEach((mutation) => {
        mutation.addedNodes.forEach((node) => {
          if (node.nodeName === 'STYLE' && !(node as HTMLStyleElement).hasAttribute('data-mf-app')) {
            (node as HTMLStyleElement).setAttribute('data-mf-app', 'shopping');
          }
        });
      });
    });

    // <head>에 추가되는 스타일 태그 감시
    styleObserver.observe(document.head, { childList: true });

    // 🆕 WeakMap에 인스턴스 등록
    instanceRegistry.set(el, {
      root,
      navigateCallback,
      styleObserver,
      isActive: true
    });

    const currentProps = {
      initialPath,
      onNavigate: (path: string) => {
        const instance = instanceRegistry.get(el);
        if (instance?.isActive) {
          console.log(`📍 [Shopping] Route changed to: ${path}`);
          instance.navigateCallback?.(path);
        }
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

    // ✅ Step 4: 앱 인스턴스 반환 (확장된 인터페이스)
    return {
      /**
       * Parent(Portal Shell)로부터 경로 변경 수신
       * Blog의 onParentNavigate와 동일한 역할
       */
      onParentNavigate: (path: string) => {
        const instance = instanceRegistry.get(el);
        if (!instance?.isActive) {
          console.log(`⏸️ [Shopping] Skipping navigation (inactive): ${path}`);
          return;
        }
        console.log(`📥 [Shopping] Received navigation from parent: ${path}`);
        navigateTo(path);
      },

      /**
       * 🆕 keep-alive activated 콜백
       * Vue의 onActivated 훅에서 호출됨
       */
      onActivated: () => {
        console.log('🔄 [Shopping] App activated (keep-alive)');
        const instance = instanceRegistry.get(el);
        if (instance) {
          instance.isActive = true;
          // data-service 복원
          document.documentElement.setAttribute('data-service', 'shopping');

          // NavigationSync 활성화 (약간의 지연으로 초기 sync 방지)
          setTimeout(() => {
            setAppActive(true);
          }, 100);
        }
      },

      /**
       * 🆕 keep-alive deactivated 콜백
       * Vue의 onDeactivated 훅에서 호출됨
       */
      onDeactivated: () => {
        console.log('⏸️ [Shopping] App deactivated (keep-alive)');
        const instance = instanceRegistry.get(el);
        if (instance) {
          instance.isActive = false;
          // NavigationSync 비활성화 (즉시)
          setAppActive(false);
        }
      },

      /**
       * 앱 언마운트 및 클린업
       * Blog의 unmount와 동일한 역할
       *
       * 🔴 핵심: <head>의 Shopping CSS 스타일 태그 제거!
       */
      unmount: () => {
        console.group('🔄 [Shopping] Unmounting app');

        const instance = instanceRegistry.get(el);

        // 1. MutationObserver 정리
        if (instance?.styleObserver) {
          instance.styleObserver.disconnect();
        }

        // 2. React Root Unmount
        try {
          if (instance?.root) {
            instance.root.unmount();
          }
          console.log('✅ [Shopping] App unmounted successfully');
        } catch (err) {
          console.error('❌ [Shopping] App unmount failed:', err);
        }

        // 3. DOM & Style Cleanup
        try {
          el.innerHTML = '';

          // 🆕 마커 기반 스타일 태그 제거 (더 정확함)
          document.querySelectorAll('style[data-mf-app="shopping"]').forEach(el => {
            console.log('   📍 [Shopping] Removing marked style tag');
            el.remove();
          });

          // 기존 방식도 유지 (fallback)
          const styleTags = document.querySelectorAll('style:not([data-mf-app])');
          styleTags.forEach((styleTag, index) => {
            const content = styleTag.textContent || '';
            if (content.includes('[data-service="shopping"]') ||
              content.includes('shopping-') ||
              (content.includes('@import') && content.includes('shopping'))) {
              console.log(`   📍 [Shopping] Found Shopping CSS at index ${index}, removing...`);
              styleTag.remove();
            }
          });

          // <link> 태그 중 Shopping CSS 제거
          const linkTags = document.querySelectorAll('link[rel="stylesheet"]');
          linkTags.forEach((linkTag) => {
            const href = linkTag.getAttribute('href') || '';
            if (href.includes('shopping') || href.includes('shopping-frontend')) {
              console.log(`   📍 [Shopping] Found Shopping CSS link: ${href}, removing...`);
              linkTag.remove();
            }
          });

          // data-service 속성 정리
          if (document.documentElement.getAttribute('data-service') === 'shopping') {
            console.log('   📍 [Shopping] Resetting data-service attribute...');
            document.documentElement.removeAttribute('data-service');
          }

          // 🆕 Router 상태 리셋
          resetRouter();

          // Portal Shell 플래그 리셋 (다른 앱 영향 방지)
          // Note: 다른 remote 앱이 아직 마운트되어 있을 수 있으므로 주석 처리
          // delete (window as any).__POWERED_BY_PORTAL_SHELL__;

          console.log('✅ [Shopping] Cleanup completed');
        } catch (err) {
          console.error('❌ [Shopping] Cleanup failed:', err);
        }

        // 4. WeakMap에서 제거
        instanceRegistry.delete(el);

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
