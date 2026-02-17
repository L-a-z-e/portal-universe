import { useEffect } from 'react'
import { SellerRouter } from '@/router'
import { usePortalTheme } from '@portal/react-bridge'
import { ToastContainer, useToast, ErrorBoundary } from '@portal/design-react'
import './styles/index.css'

interface AppProps {
  theme?: 'light' | 'dark'
  locale?: string
  userRole?: 'guest' | 'user' | 'admin'
  initialPath?: string
  onNavigate?: (path: string) => void
}

function App({
  theme = 'light',
  locale: _locale = 'ko',
  userRole: _userRole = 'guest',
  initialPath = '/',
  onNavigate,
}: AppProps) {
  const { toasts, removeToast } = useToast()
  const isEmbedded = window.__POWERED_BY_PORTAL_SHELL__ === true
  const portalTheme = usePortalTheme()

  const isDark = isEmbedded && portalTheme.isConnected
    ? portalTheme.isDark
    : theme === 'dark'

  const updateDataTheme = (isDark: boolean) => {
    const themeValue = isDark ? 'dark' : 'light'
    document.documentElement.setAttribute('data-theme', themeValue)
  }

  useEffect(() => {
    if (isDark) {
      document.documentElement.classList.add('dark')
    } else {
      document.documentElement.classList.remove('dark')
    }
    updateDataTheme(isDark)
  }, [isDark, isEmbedded, portalTheme.isConnected])

  useEffect(() => {
    document.documentElement.setAttribute('data-service', 'shopping')
  }, [])

  return (
    <ErrorBoundary moduleName="ShoppingSeller">
      <div className="min-h-screen bg-bg-page">
        <ToastContainer toasts={toasts} onDismiss={removeToast} />
        {!isEmbedded && (
          <header className="bg-bg-card border-b border-border-default sticky top-0 z-50">
            <div className="max-w-7xl mx-auto px-4 py-4">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-lg bg-gradient-to-br from-brand-primary to-brand-secondary flex items-center justify-center shadow-lg">
                    <span className="text-white font-bold text-lg">S</span>
                  </div>
                  <span className="text-xl font-bold text-text-heading">Seller Portal</span>
                </div>
                <div className="px-3 py-1 bg-status-success-bg text-status-success text-sm font-medium rounded-full border border-status-success/20">
                  Standalone
                </div>
              </div>
            </div>
          </header>
        )}
        <main className={isEmbedded ? '' : ''}>
          <SellerRouter
            isEmbedded={isEmbedded}
            initialPath={initialPath}
            onNavigate={onNavigate}
          />
        </main>
      </div>
    </ErrorBoundary>
  )
}

export default App
