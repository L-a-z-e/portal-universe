/**
 * usePortalBridge - Bridge 초기화 상태 확인용 Hook
 */
import { isBridgeReady } from '../bridge-registry'

export function usePortalBridge(): boolean {
  return isBridgeReady() || !window.__POWERED_BY_PORTAL_SHELL__
}
