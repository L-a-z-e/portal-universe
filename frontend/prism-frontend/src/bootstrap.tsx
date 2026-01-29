/// <reference types="vite/client" />
import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';
import { navigateTo, resetRouter, setAppActive } from './router';
import './index.css';

/**
 * Mount 옵션 (Shopping/Blog와 동일한 인터페이스)
 */
export type MountOptions = {
  /** 초기 경로 */
  initialPath?: string;
  /** Parent에게 경로 변경 알림 */
  onNavigate?: (path: string) => void;
  /** 테마 설정 */
  theme?: 'light' | 'dark';
};

/**
 * Mount된 Prism 앱 인스턴스
 */
export type PrismAppInstance = {
  /** Parent로부터 경로 변경 수신 */
  onParentNavigate: (path: string) => void;
  /** 앱 언마운트 */
  unmount: () => void;
  /** keep-alive activated 콜백 */
  onActivated?: () => void;
  /** keep-alive deactivated 콜백 */
  onDeactivated?: () => void;
  /** 테마 변경 콜백 */
  onThemeChange?: (theme: 'light' | 'dark') => void;
};

// 인스턴스별 상태 관리
const instanceRegistry = new WeakMap<HTMLElement, {
  root: ReactDOM.Root;
  navigateCallback: ((path: string) => void) | null;
  styleObserver: MutationObserver | null;
  isActive: boolean;
  currentTheme: 'light' | 'dark';
  rerender: () => void;
}>();

/**
 * Prism 앱을 지정된 컨테이너에 마운트 (Embedded 모드)
 */
export function mountPrismApp(
  el: HTMLElement,
  options: MountOptions = {}
): PrismAppInstance {
  console.group('🚀 [Prism] Mounting app in EMBEDDED mode');

  // Portal Shell에서 마운트됨을 표시
  (window as any).__POWERED_BY_PORTAL_SHELL__ = true;

  if (!el) {
    console.error('❌ [Prism] Mount element is null!');
    console.groupEnd();
    throw new Error('[Prism] Mount element is required');
  }

  // 기존 인스턴스 정리
  const existingInstance = instanceRegistry.get(el);
  if (existingInstance) {
    console.log('⚠️ [Prism] Cleaning up existing instance...');
    try {
      existingInstance.styleObserver?.disconnect();
      existingInstance.root.unmount();
    } catch (err) {
      console.warn('⚠️ [Prism] Existing instance cleanup warning:', err);
    }
    instanceRegistry.delete(el);
  }

  console.log('📍 Mount target:', el.tagName, el.className || '(no class)');

  const { initialPath = '/', onNavigate, theme = 'light' } = options;
  console.log('📍 Initial path:', initialPath);
  console.log('📍 Theme:', theme);

  try {
    const root = ReactDOM.createRoot(el);
    let navigateCallback = onNavigate || null;
    let currentTheme: 'light' | 'dark' = theme;

    // 스타일 태그 마킹을 위한 MutationObserver
    const styleObserver = new MutationObserver((mutations) => {
      mutations.forEach((mutation) => {
        mutation.addedNodes.forEach((node) => {
          if (node.nodeName === 'STYLE' && !(node as HTMLStyleElement).hasAttribute('data-mf-app')) {
            (node as HTMLStyleElement).setAttribute('data-mf-app', 'prism');
          }
        });
      });
    });

    // <head>에 추가되는 스타일 태그 감시
    styleObserver.observe(document.head, { childList: true });

    const getCurrentProps = () => ({
      initialPath,
      theme: currentTheme,
      onNavigate: (path: string) => {
        const instance = instanceRegistry.get(el);
        if (instance?.isActive) {
          console.log(`📍 [Prism] Route changed to: ${path}`);
          instance.navigateCallback?.(path);
        }
      }
    });

    const rerender = () => {
      root.render(
        <React.StrictMode>
          <App {...getCurrentProps()} />
        </React.StrictMode>
      );
    };

    instanceRegistry.set(el, {
      root,
      navigateCallback,
      styleObserver,
      isActive: true,
      currentTheme,
      rerender
    });

    // data-service 속성 설정
    document.documentElement.setAttribute('data-service', 'prism');
    console.log('[Prism] Set data-service="prism"');

    // 렌더링
    rerender();
    console.log('✅ [Prism] App mounted successfully');
    console.groupEnd();

    return {
      onParentNavigate: (path: string) => {
        const instance = instanceRegistry.get(el);
        if (!instance?.isActive) {
          console.log(`⏸️ [Prism] Skipping navigation (inactive): ${path}`);
          return;
        }
        console.log(`📥 [Prism] Received navigation from parent: ${path}`);
        navigateTo(path);
      },

      onActivated: () => {
        console.log('🔄 [Prism] App activated (keep-alive)');
        const instance = instanceRegistry.get(el);
        if (instance) {
          instance.isActive = true;
          document.documentElement.setAttribute('data-service', 'prism');

          // NavigationSync 활성화
          setTimeout(() => {
            setAppActive(true);
          }, 100);
        }
      },

      onDeactivated: () => {
        console.log('⏸️ [Prism] App deactivated (keep-alive)');
        const instance = instanceRegistry.get(el);
        if (instance) {
          instance.isActive = false;
          setAppActive(false);
        }
      },

      onThemeChange: (newTheme: 'light' | 'dark') => {
        console.log(`🎨 [Prism] Theme changed to: ${newTheme}`);
        const instance = instanceRegistry.get(el);
        if (instance) {
          currentTheme = newTheme;
          instance.currentTheme = newTheme;
          instance.rerender();
        }
      },

      unmount: () => {
        console.group('🔄 [Prism] Unmounting app');

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
          console.log('✅ [Prism] App unmounted successfully');
        } catch (err) {
          console.error('❌ [Prism] App unmount failed:', err);
        }

        // 3. DOM Cleanup
        try {
          el.innerHTML = '';

          if (document.documentElement.getAttribute('data-service') === 'prism') {
            document.documentElement.removeAttribute('data-service');
          }

          resetRouter();
          console.log('✅ [Prism] Cleanup completed');
        } catch (err) {
          console.error('❌ [Prism] Cleanup failed:', err);
        }

        // 4. WeakMap에서 제거
        instanceRegistry.delete(el);

        console.groupEnd();
      }
    };
  } catch (error) {
    console.error('❌ [Prism] Mount failed:', error);
    console.groupEnd();
    throw error;
  }
}

export default { mountPrismApp };
