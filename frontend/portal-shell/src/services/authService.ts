// portal-shell/src/services/authService.ts
/**
 * Direct JWT Authentication Service
 * - No OIDC dependency
 * - Token-based authentication
 * - Social login (Google, Naver, Kakao)
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
  private refreshToken: string | null = null;
  private refreshTokenKey = 'portal_refresh_token';

  constructor() {
    // Load refresh token from localStorage on init
    this.loadRefreshToken();
  }

  /**
   * Load refresh token from localStorage
   */
  private loadRefreshToken(): void {
    const stored = localStorage.getItem(this.refreshTokenKey);
    if (stored) {
      this.refreshToken = stored;
      console.log('✅ Refresh token loaded from localStorage');
    }
  }

  /**
   * Save refresh token to localStorage
   */
  private saveRefreshToken(token: string): void {
    this.refreshToken = token;
    localStorage.setItem(this.refreshTokenKey, token);
    console.log('✅ Refresh token saved to localStorage');
  }

  /**
   * Clear refresh token from localStorage
   */
  private clearRefreshToken(): void {
    this.refreshToken = null;
    localStorage.removeItem(this.refreshTokenKey);
    console.log('✅ Refresh token cleared from localStorage');
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
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || `Login failed: ${response.status}`);
      }

      const result = await response.json();

      // Backend returns { success, data: { accessToken, refreshToken, expiresIn } }
      const data: AuthResponse = result.data || result;

      // Store tokens
      this.setTokens(data.accessToken, data.refreshToken);

      console.log('✅ Login successful');
      return data;
    } catch (error) {
      console.error('❌ Login error:', error);
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
   * Refresh access token using refresh token
   */
  async refresh(): Promise<string> {
    const apiBase = getApiBaseUrl();

    if (!this.refreshToken) {
      throw new Error('No refresh token available');
    }

    try {
      console.log('[Auth] Refreshing access token...');

      const response = await fetch(`${apiBase}/auth-service/api/v1/auth/refresh`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ refreshToken: this.refreshToken }),
      });

      if (!response.ok) {
        throw new Error(`Token refresh failed: ${response.status}`);
      }

      const result = await response.json();

      // Backend returns { success, data: { accessToken, expiresIn } }
      const data = result.data || result;

      // Update access token (refresh token stays the same)
      this.accessToken = data.accessToken;
      window.__PORTAL_ACCESS_TOKEN__ = data.accessToken;

      console.log('✅ Token refreshed successfully');
      return data.accessToken;
    } catch (error) {
      console.error('❌ Token refresh error:', error);
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

      if (this.accessToken && this.refreshToken) {
        await fetch(`${apiBase}/auth-service/api/v1/auth/logout`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${this.accessToken}`,
          },
          body: JSON.stringify({ refreshToken: this.refreshToken }),
        }).catch((err) => {
          console.warn('Logout API call failed (ignored):', err);
        });
      }

      console.log('✅ Logout successful');
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
   * Set tokens (access + refresh)
   */
  setTokens(accessToken: string, refreshToken: string): void {
    this.accessToken = accessToken;
    this.saveRefreshToken(refreshToken);

    // Set global token for remote apps
    window.__PORTAL_ACCESS_TOKEN__ = accessToken;
    console.log('✅ Tokens set (access token in memory, refresh token in localStorage)');
  }

  /**
   * Clear all tokens
   */
  clearTokens(): void {
    this.accessToken = null;
    this.clearRefreshToken();

    // Clear global token
    delete window.__PORTAL_ACCESS_TOKEN__;
    console.log('✅ All tokens cleared');
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
   * Auto-refresh if token is about to expire
   */
  async autoRefreshIfNeeded(): Promise<void> {
    if (this.isTokenExpired() && this.refreshToken) {
      try {
        await this.refresh();
      } catch (error) {
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

export function setTokens(accessToken: string, refreshToken: string): void {
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
