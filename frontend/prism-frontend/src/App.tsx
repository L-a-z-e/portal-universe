/**
 * Prism Frontend - AI Agent Orchestration App
 *
 * Portal Shell과 연동되는 Module Federation Remote 앱
 * Shopping Frontend와 동일한 패턴으로 구현
 */
import { useEffect } from 'react';
import { PrismRouter } from '@/router';
import { usePortalTheme } from '@portal/react-bridge';
import { ToastContainer, useToast } from '@portal/design-system-react';
import ErrorBoundary from '@/components/ErrorBoundary';

/**
 * App Props 인터페이스
 * Portal Shell(Host)에서 전달받는 Props
 */
interface AppProps {
  /** 테마 설정 */
  theme?: 'light' | 'dark';
  /** 언어/로케일 설정 */
  locale?: string;
  /** 사용자 역할 */
  userRole?: 'guest' | 'user' | 'admin';
  /** 초기 라우트 경로 */
  initialPath?: string;
  /** 라우트 변경 콜백 (Parent에게 알림) */
  onNavigate?: (path: string) => void;
}

/**
 * Prism Frontend 루트 컴포넌트
 *
 * 특징:
 * - Portal Shell과 Props 기반으로 통신
 * - data-service="prism" CSS 활성화
 * - data-theme 속성으로 테마 동기화
 * - Portal Shell의 themeStore와 연동 (Embedded 모드)
 */
function App({
  theme = 'light',
  locale = 'ko',
  userRole = 'guest',
  initialPath = '/',
  onNavigate,
}: AppProps) {
  // ============================================
  // State 정의
  // ============================================

  /** Toast 상태 (글로벌) */
  const { toasts, removeToast } = useToast();

  /** Portal Shell과의 연동 여부 */
  const isEmbedded = window.__POWERED_BY_PORTAL_SHELL__ === true;

  /** Portal Shell 테마 상태 (Embedded 모드에서 사용) */
  const portalTheme = usePortalTheme();

  /**
   * 현재 적용할 테마 결정
   * - Embedded 모드 & adapter 연결됨: Portal adapter의 isDark 사용
   * - 그 외: props.theme 사용
   */
  const isDark = isEmbedded && portalTheme.isConnected
    ? portalTheme.isDark
    : theme === 'dark';

  // ============================================
  // Helper 함수
  // ============================================

  /**
   * data-theme 속성 동기화
   */
  const updateDataTheme = (isDark: boolean) => {
    const themeValue = isDark ? 'dark' : 'light';
    document.documentElement.setAttribute('data-theme', themeValue);
    console.log(`[Prism] Theme synced: data-theme="${themeValue}"`);
  };

  // ============================================
  // 생명주기 훅
  // ============================================

  /**
   * 테마 변경 감지 및 적용
   */
  useEffect(() => {
    if (isDark) {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
    updateDataTheme(isDark);
    console.log(`[Prism] Theme applied: ${isDark ? 'dark' : 'light'} (source: ${
      isEmbedded && portalTheme.isConnected ? 'Portal adapter' : 'props'
    })`);
  }, [isDark, isEmbedded, portalTheme.isConnected]);

  /**
   * 마운트 및 초기 설정
   */
  useEffect(() => {
    console.group('🔧 [Prism] App mounted with props:');
    console.log('  theme:', theme);
    console.log('  locale:', locale);
    console.log('  isEmbedded:', isEmbedded);
    console.log('  portalTheme.isConnected:', portalTheme.isConnected);
    console.groupEnd();

    // ✅ Step 1: data-service="prism" 속성 설정 (CSS 선택자 활성화)
    document.documentElement.setAttribute('data-service', 'prism');

    // Auth 동기화는 PortalBridgeProvider + usePortalAuth가 처리
  }, [theme, locale, userRole, isEmbedded, portalTheme.isConnected]);

  // ============================================
  // 렌더링
  // ============================================

  return (
    <ErrorBoundary>
    <div className="min-h-screen bg-bg-page">
      <ToastContainer toasts={toasts} onDismiss={removeToast} />
      {/* Header (Standalone 모드에서만 표시) */}
      {!isEmbedded && (
        <header className="bg-bg-card border-b border-border-default sticky top-0 z-50">
          <div className="max-w-7xl mx-auto px-4 py-4">
            <div className="flex items-center justify-between">
              {/* Logo */}
              <div className="flex items-center gap-3 hover:opacity-80 transition-opacity cursor-pointer">
                <div className="w-10 h-10 rounded-lg bg-gradient-to-br from-brand-primary to-brand-primaryHover flex items-center justify-center shadow-lg">
                  <span className="text-white font-bold text-lg">P</span>
                </div>
                <span className="text-xl font-bold text-text-heading">Prism</span>
              </div>

              {/* Nav */}
              <nav className="flex items-center gap-6">
                <a
                  href="/"
                  className="text-text-body hover:text-brand-primary font-medium transition-colors"
                >
                  Boards
                </a>
                <a
                  href="/agents"
                  className="text-text-body hover:text-brand-primary font-medium transition-colors"
                >
                  Agents
                </a>
                <a
                  href="/providers"
                  className="text-text-body hover:text-brand-primary font-medium transition-colors"
                >
                  Providers
                </a>
              </nav>

              {/* Mode Badge (Standalone) */}
              <div className="px-3 py-1 bg-status-success-bg text-status-success text-sm font-medium rounded-full border border-status-success/20">
                Standalone
              </div>
            </div>
          </div>
        </header>
      )}

      {/* Main Content */}
      <main className={isEmbedded ? 'py-4' : 'py-8'}>
        <div className="max-w-7xl mx-auto px-6">
          <PrismRouter
            isEmbedded={isEmbedded}
            initialPath={initialPath}
            onNavigate={onNavigate}
          />
        </div>
      </main>

      {/* Footer (Standalone 모드에서만) */}
      {!isEmbedded && (
        <footer className="bg-bg-card border-t border-border-default mt-auto">
          <div className="max-w-7xl mx-auto px-4 py-6 text-center">
            <p className="text-sm text-text-meta">
              © 2025 Portal Universe Prism. All rights reserved.
            </p>
          </div>
        </footer>
      )}
    </div>
    </ErrorBoundary>
  );
}

export default App;
