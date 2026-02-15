/**
 * Portal Shell에 의해 마운트된 Embedded 모드인지 확인
 */
export function isEmbedded(): boolean {
  return (window as unknown as Record<string, unknown>).__POWERED_BY_PORTAL_SHELL__ === true
}
