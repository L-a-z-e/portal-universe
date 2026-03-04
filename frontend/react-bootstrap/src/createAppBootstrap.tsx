import React from 'react';
import ReactDOM from 'react-dom/client';
import { PortalBridgeProvider } from '@portal/react-bridge';
import type {
  AppBootstrapConfig,
  MountOptions,
  AppInstance,
  AppInstanceState,
  Theme,
} from './types';

/**
 * 마운트된 앱 인스턴스 상태 저장소
 *
 * WeakMap을 사용하는 이유:
 * - key(HTMLElement)가 DOM에서 제거되면 자동으로 GC됨
 * - 메모리 누수 방지
 */
const mountedAppStates = new WeakMap<HTMLElement, AppInstanceState>();

/**
 * React 마이크로프론트엔드 앱의 부트스트랩 함수를 생성합니다.
 *
 * @description
 * 이 팩토리 함수는 shopping-frontend, prism-frontend 등
 * 여러 React 앱에서 공통으로 사용되는 마운트/언마운트 로직을
 * 재사용 가능하게 만들어줍니다.
 *
 * @example
 * ```tsx
 * // shopping-frontend/src/bootstrap.tsx
 * import { createAppBootstrap } from '@portal/react-bootstrap';
 * import App from './App';
 *
 * export const { mount, unmount } = createAppBootstrap({
 *   name: 'shopping',
 *   App,
 *   dataService: 'shopping',
 * });
 * ```
 *
 * @param config - 앱 설정 (이름, 컴포넌트, data-service 값)
 * @returns mount 함수와 unmount 헬퍼
 */
export function createAppBootstrap(config: AppBootstrapConfig) {
  const { name, App, dataService, router } = config;

  /**
   * 앱을 지정된 컨테이너에 마운트합니다.
   *
   * @param el - 마운트할 HTML 엘리먼트
   * @param options - 마운트 옵션 (initialPath, theme, onNavigate)
   * @returns 앱 인스턴스 (onParentNavigate, unmount, onActivated 등)
   */
  function mount(el: HTMLElement, options: MountOptions = {}): AppInstance {
    // Portal Shell에서 마운트됨을 표시
    (window as any).__POWERED_BY_PORTAL_SHELL__ = true;

    // 필수 파라미터 검증
    if (!el) {
      throw new Error(`[${name}] Mount element is required`);
    }

    // 기존 인스턴스가 있으면 정리
    const existingState = mountedAppStates.get(el);
    if (existingState) {
      cleanupInstance(el, existingState);
    }

    const { initialPath = '/', onNavigate, theme = 'light' } = options;

    try {
      // Step 1: React 루트 생성
      const root = ReactDOM.createRoot(el);
      let currentTheme: Theme = theme;
      const navigateCallback = onNavigate || null;

      // Step 2: 스타일 태그 마킹을 위한 MutationObserver
      // (Module Federation에서 스타일 충돌 방지)
      const styleObserver = createStyleObserver(dataService);

      // Step 3: Props 생성 함수
      const getCurrentProps = () => ({
        initialPath,
        theme: currentTheme,
        onNavigate: (path: string) => {
          const state = mountedAppStates.get(el);
          if (state?.isActive) {
            state.navigateCallback?.(path);
          }
        },
      });

      // Step 4: 렌더링 함수
      const rerender = () => {
        root.render(
          <React.StrictMode>
            <PortalBridgeProvider>
              <App {...getCurrentProps()} />
            </PortalBridgeProvider>
          </React.StrictMode>
        );
      };

      // Step 5: 상태 저장
      mountedAppStates.set(el, {
        root,
        navigateCallback,
        styleObserver,
        isActive: true,
        currentTheme,
        rerender,
      });

      // Step 6: data-service 속성 설정 (CSS 선택자 활성화)
      document.documentElement.setAttribute('data-service', dataService);

      // Step 7: 초기 렌더링
      rerender();

      // Step 8: 앱 인스턴스 반환
      return createAppInstance(el, name, dataService, router, () => currentTheme, (t) => { currentTheme = t; });

    } catch (error) {
      console.error(`[${name}] Mount failed:`, error);
      throw error;
    }
  }

  return { mount };
}

/**
 * 스타일 태그 마킹을 위한 MutationObserver 생성
 *
 * Module Federation에서 여러 앱의 스타일이 <head>에 추가될 때
 * 어떤 앱의 스타일인지 구분하기 위해 data-mf-app 속성을 추가합니다.
 */
function createStyleObserver(appName: string): MutationObserver {
  const observer = new MutationObserver((mutations) => {
    mutations.forEach((mutation) => {
      mutation.addedNodes.forEach((node) => {
        if (
          node.nodeName === 'STYLE' &&
          !(node as HTMLStyleElement).hasAttribute('data-mf-app')
        ) {
          (node as HTMLStyleElement).setAttribute('data-mf-app', appName);
        }
      });
    });
  });

  observer.observe(document.head, { childList: true });
  return observer;
}

/**
 * 앱 인스턴스 생성
 *
 * Portal Shell에서 호출할 수 있는 메서드들을 제공합니다:
 * - onParentNavigate: 부모에서 경로 변경 시
 * - onActivated/onDeactivated: keep-alive 상태 변경 시
 * - onThemeChange: 테마 변경 시
 * - unmount: 앱 제거 시
 */
function createAppInstance(
  el: HTMLElement,
  name: string,
  dataService: string,
  router: AppBootstrapConfig['router'],
  _getTheme: () => Theme,  // 향후 확장용으로 유지
  setTheme: (t: Theme) => void
): AppInstance {
  return {
    onParentNavigate: (path: string) => {
      const state = mountedAppStates.get(el);
      if (!state?.isActive) {
        return;
      }
      router?.navigateTo(path);
    },

    onActivated: () => {
      const state = mountedAppStates.get(el);
      if (state) {
        state.isActive = true;
        document.documentElement.setAttribute('data-service', dataService);
        setTimeout(() => router?.setAppActive(true), 100);
      }
    },

    onDeactivated: () => {
      const state = mountedAppStates.get(el);
      if (state) {
        state.isActive = false;
        router?.setAppActive(false);
      }
    },

    onThemeChange: (newTheme: Theme) => {
      const state = mountedAppStates.get(el);
      if (state) {
        setTheme(newTheme);
        state.currentTheme = newTheme;
        state.rerender();
      }
    },

    unmount: () => {
      const state = mountedAppStates.get(el);

      if (state) {
        cleanupInstance(el, state);
      }

      // DOM 정리
      try {
        el.innerHTML = '';
        if (document.documentElement.getAttribute('data-service') === dataService) {
          document.documentElement.removeAttribute('data-service');
        }
        router?.resetRouter();
      } catch (err) {
        console.error(`[${name}] Cleanup failed:`, err);
      }
    },
  };
}

/**
 * 인스턴스 정리 (내부 헬퍼)
 */
function cleanupInstance(el: HTMLElement, state: AppInstanceState): void {
  try {
    state.styleObserver?.disconnect();
    state.root.unmount();
  } catch (err) {
    console.warn('Cleanup warning:', err);
  }
  mountedAppStates.delete(el);
}
