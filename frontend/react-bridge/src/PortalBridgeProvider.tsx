/**
 * PortalBridgeProvider - Async MF Resolution + 초기화
 *
 * Race condition 해결:
 * bridge 초기화 완료 전까지 children을 렌더링하지 않으므로
 * RequireAuth의 5초 timeout hack이 필요 없어짐.
 */
import { useState, useEffect } from 'react'
import type { ReactNode } from 'react'
import { initBridge, isBridgeReady } from './bridge-registry'

interface PortalBridgeProviderProps {
  children: ReactNode
  fallback?: ReactNode
}

export function PortalBridgeProvider({ children, fallback }: PortalBridgeProviderProps) {
  const [ready, setReady] = useState(isBridgeReady())
  const [error, setError] = useState<Error | null>(null)

  useEffect(() => {
    // Standalone 모드: bridge 필요 없음
    if (!window.__POWERED_BY_PORTAL_SHELL__) {
      setReady(true)
      return
    }

    // 이미 초기화됨
    if (isBridgeReady()) {
      setReady(true)
      return
    }

    initBridge()
      .then(() => setReady(true))
      .catch((err) => {
        console.error('[PortalBridgeProvider] Failed to initialize bridge:', err)
        setError(err)
      })
  }, [])

  if (error) {
    console.warn('[PortalBridgeProvider] Bridge init failed, rendering fallback')
    return <>{fallback ?? null}</>
  }

  if (!ready) {
    return <>{fallback ?? null}</>
  }

  return <>{children}</>
}
