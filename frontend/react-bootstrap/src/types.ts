import type { ComponentType } from 'react';

/**
 * 앱 부트스트랩 설정
 *
 * @example
 * ```ts
 * const config: AppBootstrapConfig = {
 *   name: 'shopping',
 *   App: ShoppingApp,
 *   dataService: 'shopping',
 * };
 * ```
 */
export interface AppBootstrapConfig {
  /** 앱 이름 (로깅에 사용) */
  name: string;

  /** 메인 App 컴포넌트 */
  App: ComponentType<AppProps>;

  /** data-service 속성 값 (CSS 선택자 활성화) */
  dataService: string;

  /** 라우터 함수 (선택) */
  router?: RouterFunctions;
}

/**
 * App 컴포넌트에 전달되는 Props
 */
export interface AppProps {
  /** 초기 경로 */
  initialPath: string;
  /** 현재 테마 */
  theme: Theme;
  /** 내비게이션 콜백 */
  onNavigate: (path: string) => void;
}

/**
 * 마운트 옵션 (Portal Shell에서 전달)
 */
export interface MountOptions {
  /** 초기 경로 (예: '/cart', '/tasks') */
  initialPath?: string;
  /** Parent에게 경로 변경 알림 */
  onNavigate?: (path: string) => void;
  /** 테마 설정 */
  theme?: Theme;
}

/**
 * 마운트된 앱 인스턴스
 */
export interface AppInstance {
  /** Parent로부터 경로 변경 수신 */
  onParentNavigate: (path: string) => void;
  /** 앱 언마운트 */
  unmount: () => void;
  /** keep-alive activated 콜백 */
  onActivated?: () => void;
  /** keep-alive deactivated 콜백 */
  onDeactivated?: () => void;
  /** 테마 변경 콜백 */
  onThemeChange?: (theme: Theme) => void;
}

/**
 * 테마 타입
 */
export type Theme = 'light' | 'dark';

/**
 * 라우터 함수들
 */
export interface RouterFunctions {
  navigateTo: (path: string) => void;
  resetRouter: () => void;
  setAppActive: (active: boolean) => void;
}

/**
 * 인스턴스 상태 (내부 사용)
 */
export interface AppInstanceState {
  root: import('react-dom/client').Root;
  navigateCallback: ((path: string) => void) | null;
  styleObserver: MutationObserver | null;
  isActive: boolean;
  currentTheme: Theme;
  rerender: () => void;
}
