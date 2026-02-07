/**
 * Service availability check utility for E2E tests.
 * Use to skip tests gracefully when required services are not running.
 */

export async function checkServiceAvailable(url: string, timeout = 5000): Promise<boolean> {
  try {
    const resp = await fetch(url, { signal: AbortSignal.timeout(timeout) })
    return resp.ok || resp.status < 500
  } catch {
    return false
  }
}

export const SERVICES = {
  portalShell: 'http://localhost:30000',
  blogFrontend: 'http://localhost:30001',
  shoppingFrontend: 'http://localhost:30002',
  prismFrontend: 'http://localhost:30003',
  adminFrontend: 'http://localhost:30004',
  authService: 'http://localhost:8081/actuator/health',
  blogService: 'http://localhost:8082/actuator/health',
  shoppingService: 'http://localhost:8083/actuator/health',
  prismService: 'http://localhost:8085/api/health',
  ollama: 'http://localhost:11434',
} as const
