import React, { useEffect, useState } from 'react'
import './App.scss'

/**
 * App Props 인터페이스
 * Portal Shell(Host)에서 전달받는 Props
 */
interface AppProps {
  /** 테마 설정 */
  theme?: 'light' | 'dark'
  /** 언어/로케일 설정 */
  locale?: string
  /** 사용자 역할 */
  userRole?: 'guest' | 'user' | 'admin'
  /** 기타 Props */
  [key: string]: any
}

/**
 * Shopping Frontend 루트 컴포넌트
 *
 * 특징:
 * - Portal Shell과 Props 기반으로 통신
 * - data-service="shopping" CSS 활성화
 * - data-theme 속성으로 테마 동기화
 * - Portal Shell의 themeStore와 연동 (Embedded 모드)
 */
function App({
               theme = 'light',
               locale = 'ko',
               userRole = 'guest',
               ...otherProps
             }: AppProps) {
  // ============================================
  // State 정의
  // ============================================

  /** Portal Shell과의 연동 여부 */
  const isEmbedded = window.__POWERED_BY_PORTAL_SHELL__ === true

  /** Portal Shell의 themeStore (동적 import 후 저장) */
  const [themeStore, setThemeStore] = useState<any>(null)

  // ============================================
  // Helper 함수
  // ============================================

  /**
   * data-theme 속성 동기화
   * - <html class="dark"> → <html data-theme="dark">
   * - [data-theme="dark"] CSS 선택자 활성화
   * - [data-service="shopping"][data-theme="dark"] 서비스별 다크 테마 활성화
   *
   * Blog의 updateDataTheme() 패턴 적용
   */
  const updateDataTheme = (isDark: boolean) => {
    const themeValue = isDark ? 'dark' : 'light'
    document.documentElement.setAttribute('data-theme', themeValue)
    console.log(`[Shopping] Theme synced: data-theme="${themeValue}"`)
  }

  // ============================================
  // 생명주기 훅
  // ============================================

  /**
   * 마운트 및 Props 변화 감지
   * Blog의 onMounted와 watch 로직 적용
   */
  useEffect(() => {
    console.group('🔧 [Shopping] App mounted with props:')
    console.log('  theme:', theme)
    console.log('  locale:', locale)
    console.log('  userRole:', userRole)
    console.log('  otherProps:', otherProps)
    console.groupEnd()

    // ✅ Step 1: data-service="shopping" 속성 설정 (CSS 선택자 활성화)
    document.documentElement.setAttribute('data-service', 'shopping')
    console.log('[Shopping] Set data-service="shopping"')

    // ✅ Step 2: 초기 data-theme 설정
    const isDark = theme === 'dark'
    updateDataTheme(isDark)

    if (isEmbedded) {
      // ============================================
      // Embedded 모드: Portal Shell의 themeStore 연동
      // ============================================
      console.log('[Shopping] Embedded mode detected - connecting to Portal Shell...')

      /**
       * Portal Shell의 themeStore 동적 import
       * Blog의 import('portal_shell/themeStore') 패턴 적용
       */
      import('portal/themeStore')
        .then(({ useThemeStore }) => {
          try {
            const store = useThemeStore()
            setThemeStore(store)

            // ✅ Step 3: 초기 다크모드 적용
            if (store.isDark) {
              document.documentElement.classList.add('dark')
            } else {
              document.documentElement.classList.remove('dark')
            }
            updateDataTheme(store.isDark)

            console.log('[Shopping] Portal Shell themeStore connected')
            console.log('  isDark:', store.isDark)
          } catch (err) {
            console.error('[Shopping] Failed to initialize themeStore:', err)
          }
        })
        .catch((err) => {
          console.warn('[Shopping] Failed to load portal/themeStore:', err)
          console.warn('[Shopping] Fallback: Using local theme prop')
        })
    } else {
      // ============================================
      // Standalone 모드: MutationObserver로 dark 클래스 감지
      // ============================================
      console.log('[Shopping] Standalone mode - using MutationObserver...')

      const observer = new MutationObserver((mutations) => {
        mutations.forEach((mutation) => {
          if (mutation.attributeName === 'class') {
            const isDark = document.documentElement.classList.contains('dark')
            updateDataTheme(isDark)
          }
        })
      })

      observer.observe(document.documentElement, {
        attributes: true,
        attributeFilter: ['class']
      })

      console.log('[Shopping] Standalone mode: MutationObserver registered')

      // Cleanup
      return () => {
        observer.disconnect()
      }
    }
  }, [theme, locale, userRole, otherProps, isEmbedded])

  /**
   * themeStore 변화 감지 (Embedded 모드)
   * Blog의 watch(themeStore.isDark) 패턴 적용
   */
  useEffect(() => {
    if (!themeStore || !isEmbedded) return

    // themeStore 감시는 themeStore 자체에서 처리
    // 여기서는 Props로 전달받은 theme 변화를 처리

    console.log('[Shopping] Theme prop changed:', theme)

    const isDark = theme === 'dark'
    if (isDark) {
      document.documentElement.classList.add('dark')
    } else {
      document.documentElement.classList.remove('dark')
    }
    updateDataTheme(isDark)

  }, [theme, themeStore, isEmbedded])

  // ============================================
  // 렌더링
  // ============================================

  return (
    <>
      {/* ✅ data-service="shopping" 자동으로 설정됨 (JS에서) */}
      {/* ✅ data-theme 속성도 JS에서 자동 설정됨 */}

      <div className="min-h-screen bg-bg-page">
        {/* Header (Standalone 모드에서만 표시) */}
        {!isEmbedded && (
          <header className="bg-bg-card border-b border-border-default sticky top-0 z-50">
            <div className="max-w-7xl mx-auto px-4 py-4">
              <div className="flex items-center justify-between">
                {/* Logo */}
                <div className="flex items-center gap-3 hover:opacity-80 transition-opacity cursor-pointer">
                  <div className="w-10 h-10 rounded-lg bg-gradient-to-br from-brand-primary to-brand-secondary flex items-center justify-center shadow-lg">
                    <span className="text-white font-bold text-lg">S</span>
                  </div>
                  <span className="text-xl font-bold text-text-heading">Shopping</span>
                </div>

                {/* Nav */}
                <nav className="flex items-center gap-6">
                  <a
                    href="/"
                    className="text-text-body hover:text-brand-primary font-medium transition-colors"
                  >
                    🛍️ Products
                  </a>
                  <a
                    href="/cart"
                    className="text-text-body hover:text-brand-primary font-medium transition-colors"
                  >
                    🛒 Cart
                  </a>
                  <a
                    href="/orders"
                    className="text-text-body hover:text-brand-primary font-medium transition-colors"
                  >
                    📦 Orders
                  </a>
                </nav>

                {/* Mode Badge (Standalone) */}
                <div className="px-3 py-1 bg-status-success-bg text-status-success text-sm font-medium rounded-full border border-status-success/20">
                  📦 Standalone
                </div>
              </div>
            </div>
          </header>
        )}

        {/* Embedded Mode Badge */}
        {isEmbedded && (
          <div className="bg-status-warning-bg border-b border-status-warning/20">
            <div className="max-w-7xl mx-auto px-4 py-2">
              <p className="text-xs text-status-warning font-medium">
                🔗 Embedded Mode (Portal Shell)
              </p>
            </div>
          </div>
        )}

        {/* Main Content */}
        <main className={isEmbedded ? 'py-4' : 'py-8'}>
          <div className="max-w-7xl mx-auto px-6">
            {/* 현재 Props 표시 (개발 모드) */}
            {process.env.NODE_ENV === 'development' && (
              <div className="mb-8 p-4 bg-bg-card border border-border-default rounded-lg">
                <h2 className="text-lg font-bold text-text-heading mb-4">
                  📊 Current Props
                </h2>
                <div className="space-y-2 text-sm text-text-body font-mono">
                  <div>
                    <span className="text-text-meta">theme:</span>{' '}
                    <span className="text-status-info">{theme}</span>
                  </div>
                  <div>
                    <span className="text-text-meta">locale:</span>{' '}
                    <span className="text-status-info">{locale}</span>
                  </div>
                  <div>
                    <span className="text-text-meta">userRole:</span>{' '}
                    <span className="text-status-info">{userRole}</span>
                  </div>
                  <div>
                    <span className="text-text-meta">isEmbedded:</span>{' '}
                    <span className="text-status-info">{isEmbedded ? 'true' : 'false'}</span>
                  </div>
                </div>
              </div>
            )}

            {/* Shopping 콘텐츠 */}
            <div className="bg-bg-card border border-border-default rounded-lg p-8">
              <h1 className="text-3xl font-bold text-text-heading mb-4">
                🛍️ Shopping Frontend
              </h1>

              <div className="space-y-4 text-text-body">
                <p>
                  Welcome to the Shopping Frontend! This is a React-based micro frontend that communicates with Portal Shell using Props.
                </p>

                <div className="bg-bg-subtle p-4 rounded-lg border border-border-default">
                  <h2 className="font-bold text-text-heading mb-2">Current Configuration:</h2>
                  <ul className="list-disc list-inside space-y-1 text-sm">
                    <li>
                      <strong>Theme:</strong> {theme}
                    </li>
                    <li>
                      <strong>Locale:</strong> {locale}
                    </li>
                    <li>
                      <strong>User Role:</strong> {userRole}
                    </li>
                    <li>
                      <strong>Mode:</strong> {isEmbedded ? '🔗 Embedded' : '📦 Standalone'}
                    </li>
                  </ul>
                </div>

                <div className="bg-status-info-bg border border-status-info/20 p-4 rounded-lg">
                  <p className="text-status-info text-sm">
                    💡 <strong>Tip:</strong> Props are updated automatically when Portal Shell's state changes. The theme, locale, and user role above will update in real-time.
                  </p>
                </div>
              </div>
            </div>
          </div>
        </main>

        {/* Footer (Standalone 모드에서만) */}
        {!isEmbedded && (
          <footer className="bg-bg-card border-t border-border-default mt-auto">
            <div className="max-w-7xl mx-auto px-4 py-6 text-center">
              <p className="text-sm text-text-meta">
                © 2025 Portal Universe Shopping. All rights reserved.
              </p>
            </div>
          </footer>
        )}
      </div>
    </>
  )
}

export default App