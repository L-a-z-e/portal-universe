// portal-shell/src/services/authService.ts
/**
 * Direct JWT Authentication Service
 * - No OIDC dependency
 * - Token-based authentication
 * - Social login (Google, Naver, Kakao)
 * - Refresh Token은 HttpOnly Cookie로 관리 (XSS 방어)
 */

import { parseJwtPayload } from '../utils/jwt';

// ====================================================================
// Types
// ====================================================================

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

export interface UserInfo {
  uuid: string;
  email: string;
  username?: string;
  name?: string;
  nickname?: string;
  picture?: string;
  roles: string[];
  scopes: string[];
  memberships: Record<string, string>;
}

// ====================================================================
// Configuration
// ====================================================================

function getApiBaseUrl(): string {
  return import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
}

// ====================================================================
// Authentication Service
// ====================================================================

class AuthenticationService {
  private accessToken: string | null = null;
  private hasRefreshToken = false;
  private refreshPromise: Promise<string> | null = null;

  constructor() {
    // 기존 localStorage의 refresh token 제거 (마이그레이션)
    localStorage.removeItem('portal_refresh_token');

    // Register getter function for remote apps
    window.__PORTAL_GET_ACCESS_TOKEN__ = () => this.accessToken;
  }

  /**
   * General login (email + password)
   */
  async login(email: string, password: string): Promise<AuthResponse> {
    const apiBase = getApiBaseUrl();

    try {
      console.log('[Auth] Attempting login:', email);

      const response = await fetch(`${apiBase}/auth-service/api/v1/auth/login`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ email, password }),
        credentials: 'include',
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || `Login failed: ${response.status}`);
      }

      const result = await response.json();

      // Backend returns { success, data: { accessToken, refreshToken, expiresIn } }
      const data: AuthResponse = result.data || result;

      // Store access token in memory, refresh token is in HttpOnly cookie
      this.accessToken = data.accessToken;
      this.hasRefreshToken = true;
      window.__PORTAL_ACCESS_TOKEN__ = data.accessToken;
      window.__PORTAL_GET_ACCESS_TOKEN__ = () => this.accessToken;

      console.log('[Auth] Login successful');
      return data;
    } catch (error) {
      console.error('[Auth] Login error:', error);
      throw error;
    }
  }

  /**
   * Social login (redirect to OAuth2 authorization endpoint)
   */
  socialLogin(provider: 'google' | 'naver' | 'kakao'): void {
    const apiBase = getApiBaseUrl();
    const redirectUrl = `${apiBase}/auth-service/oauth2/authorization/${provider}`;

    console.log(`[Auth] Redirecting to ${provider} login:`, redirectUrl);
    window.location.href = redirectUrl;
  }

  /**
   * Refresh access token using refresh token (HttpOnly cookie).
   * Deduplicates concurrent refresh requests by reusing the same promise.
   */
  async refresh(): Promise<string> {
    if (this.refreshPromise) {
      return this.refreshPromise;
    }

    this.refreshPromise = this._doRefresh().finally(() => {
      this.refreshPromise = null;
    });

    return this.refreshPromise;
  }

  /**
   * Internal refresh implementation
   */
  private async _doRefresh(): Promise<string> {
    const apiBase = getApiBaseUrl();

    try {
      console.log('[Auth] Refreshing access token...');

      const response = await fetch(`${apiBase}/auth-service/api/v1/auth/refresh`, {
        method: 'POST',
        credentials: 'include',
      });

      if (!response.ok) {
        throw new Error(`Token refresh failed: ${response.status}`);
      }

      const result = await response.json();

      // Backend returns { success, data: { accessToken, refreshToken, expiresIn } }
      const data = result.data || result;

      // Update access token
      this.accessToken = data.accessToken;
      this.hasRefreshToken = true;
      window.__PORTAL_ACCESS_TOKEN__ = data.accessToken;

      console.log('[Auth] Token refreshed successfully');
      return data.accessToken;
    } catch (error) {
      console.error('[Auth] Token refresh error:', error);
      // Clear tokens on refresh failure
      this.clearTokens();
      throw error;
    }
  }

  /**
   * Logout
   */
  async logout(): Promise<void> {
    const apiBase = getApiBaseUrl();

    try {
      console.log('[Auth] Logging out...');

      if (this.accessToken) {
        await fetch(`${apiBase}/auth-service/api/v1/auth/logout`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${this.accessToken}`,
          },
          credentials: 'include',
        }).catch((err) => {
          console.warn('Logout API call failed (ignored):', err);
        });
      }

      console.log('[Auth] Logout successful');
    } finally {
      // Always clear tokens locally
      this.clearTokens();
    }
  }

  /**
   * Get access token
   */
  getAccessToken(): string | null {
    return this.accessToken;
  }

  /**
   * Set only the access token (without changing refresh token).
   * Used when backend returns a new token after profile/membership changes.
   */
  setAccessTokenOnly(token: string): void {
    this.accessToken = token;
    window.__PORTAL_ACCESS_TOKEN__ = token;
  }

  /**
   * Set tokens from external source (e.g., OAuth2 callback).
   * Access token is stored in memory.
   * Refresh token is already in HttpOnly cookie - just track its existence.
   */
  setTokens(accessToken: string, _refreshToken?: string): void {
    this.accessToken = accessToken;
    this.hasRefreshToken = true;

    // Set global token for remote apps (legacy + getter function)
    window.__PORTAL_ACCESS_TOKEN__ = accessToken;
    window.__PORTAL_GET_ACCESS_TOKEN__ = () => this.accessToken;
  }

  /**
   * Clear all tokens
   */
  clearTokens(): void {
    this.accessToken = null;
    this.hasRefreshToken = false;

    // Clear global token
    delete window.__PORTAL_ACCESS_TOKEN__;
    delete window.__PORTAL_GET_ACCESS_TOKEN__;
  }

  /**
   * Check if user is authenticated
   */
  isAuthenticated(): boolean {
    return this.accessToken !== null;
  }

  /**
   * Extract user info from JWT access token
   */
  getUserInfo(): UserInfo | null {
    if (!this.accessToken) {
      return null;
    }

    try {
      const payload = parseJwtPayload(this.accessToken);
      if (!payload) {
        return null;
      }

      return {
        uuid: payload.sub || '',
        email: payload.sub || payload.email || '',
        username: payload.preferred_username || payload.username,
        name: payload.name,
        nickname: payload.nickname,
        picture: payload.picture,
        roles: Array.isArray(payload.roles) ? payload.roles :
               payload.roles ? [payload.roles] : [],
        scopes: Array.isArray(payload.scope) ? payload.scope :
                payload.scope ? payload.scope.split(' ') : [],
        memberships: (typeof payload.memberships === 'object' && payload.memberships !== null)
                ? payload.memberships : {},
      };
    } catch (error) {
      console.error('Failed to extract user info from token:', error);
      return null;
    }
  }

  /**
   * Check if token is expired (with 60s buffer)
   */
  isTokenExpired(): boolean {
    if (!this.accessToken) {
      return true;
    }

    try {
      const payload = parseJwtPayload(this.accessToken);
      if (!payload || !payload.exp) {
        return true;
      }

      const now = Math.floor(Date.now() / 1000);
      const buffer = 60; // 60 seconds buffer
      return payload.exp < (now + buffer);
    } catch {
      return true;
    }
  }

  /**
   * Auto-refresh if token is about to expire.
   * On page reload, hasRefreshToken is false but HttpOnly cookie may exist.
   * When no access token exists, always attempt refresh (cookie-based).
   */
  async autoRefreshIfNeeded(): Promise<void> {
    const shouldRefresh = this.accessToken === null || (this.isTokenExpired() && this.hasRefreshToken);

    if (shouldRefresh) {
      try {
        await this._doRefresh();
      } catch (error) {
        // If no cookie exists, refresh will fail silently on page load
        if (this.accessToken === null) {
          console.log('[Auth] No refresh cookie available');
          return;
        }
        console.error('Auto-refresh failed:', error);
        throw error;
      }
    }
  }
}

// ====================================================================
// Singleton instance
// ====================================================================

export const authService = new AuthenticationService();

// ====================================================================
// Public API
// ====================================================================

export async function login(email: string, password: string): Promise<AuthResponse> {
  return authService.login(email, password);
}

export function socialLogin(provider: 'google' | 'naver' | 'kakao'): void {
  authService.socialLogin(provider);
}

export async function refresh(): Promise<string> {
  return authService.refresh();
}

export async function logout(): Promise<void> {
  return authService.logout();
}

export function getAccessToken(): string | null {
  return authService.getAccessToken();
}

export function setTokens(accessToken: string, refreshToken?: string): void {
  authService.setTokens(accessToken, refreshToken);
}

export function clearTokens(): void {
  authService.clearTokens();
}

export function isAuthenticated(): boolean {
  return authService.isAuthenticated();
}

export function getUserInfo(): UserInfo | null {
  return authService.getUserInfo();
}

export function isTokenExpired(): boolean {
  return authService.isTokenExpired();
}

export async function autoRefreshIfNeeded(): Promise<void> {
  return authService.autoRefreshIfNeeded();
}

// Default export
export default authService;
