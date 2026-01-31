/**
 * React Module Federation Bootstrap 공통 유틸리티
 *
 * shopping-frontend와 prism-frontend에서 공유하는 MF bootstrap 패턴을 추출.
 * 각 앱은 이 유틸리티의 타입을 참조하여 일관된 인터페이스를 유지합니다.
 *
 * 현재는 타입 정의와 문서 역할만 수행합니다.
 * 실제 공통 로직 추출은 각 앱의 빌드 의존성에 영향을 주므로,
 * 먼저 monorepo 패키지로 분리한 후 점진적으로 진행합니다.
 *
 * @see frontend/shopping-frontend/src/bootstrap.tsx
 * @see frontend/prism-frontend/src/bootstrap.tsx
 */

/**
 * Module Federation mount 옵션 (모든 React remote 앱 공통)
 */
export type MountOptions = {
  /** 초기 경로 (예: '/cart', '/orders') */
  initialPath?: string;
  /** Parent에게 경로 변경 알림 */
  onNavigate?: (path: string) => void;
  /** 테마 설정 (Portal Shell에서 전달) */
  theme?: 'light' | 'dark';
};

/**
 * Mount된 Remote 앱 인스턴스 (모든 React remote 앱 공통)
 */
export type RemoteAppInstance = {
  /** Parent로부터 경로 변경 수신 */
  onParentNavigate: (path: string) => void;
  /** 앱 언마운트 */
  unmount: () => void;
  /** keep-alive activated 콜백 */
  onActivated?: () => void;
  /** keep-alive deactivated 콜백 */
  onDeactivated?: () => void;
  /** 테마 변경 콜백 (Portal Shell에서 호출) */
  onThemeChange?: (theme: 'light' | 'dark') => void;
};

/**
 * Instance Registry에 저장되는 상태
 */
export type InstanceState = {
  root: { unmount: () => void };
  navigateCallback: ((path: string) => void) | null;
  styleObserver: MutationObserver | null;
  isActive: boolean;
  currentTheme: 'light' | 'dark';
  rerender: () => void;
};
