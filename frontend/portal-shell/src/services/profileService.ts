// portal-shell/src/services/profileService.ts
/**
 * Profile Service
 * - Profile CRUD operations
 * - Password change
 * - Account deletion
 */

import { authService } from './authService';

// ====================================================================
// Types
// ====================================================================

export interface ProfileResponse {
  uuid: string;
  email: string;
  nickname: string;
  realName: string | null;
  phoneNumber: string | null;
  profileImageUrl: string | null;
  marketingAgree: boolean;
  hasSocialAccount: boolean;
  socialProviders: string[];
  createdAt: string;
}

export interface UpdateProfileRequest {
  nickname?: string;
  realName?: string;
  phoneNumber?: string;
  profileImageUrl?: string;
  marketingAgree?: boolean;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
}

export interface DeleteAccountRequest {
  password: string;
  reason?: string;
}

// ====================================================================
// Configuration
// ====================================================================

function getApiBaseUrl(): string {
  return import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
}

// ====================================================================
// Profile Service
// ====================================================================

class ProfileServiceClass {
  /**
   * Get authenticated headers with access token
   */
  private async getAuthHeaders(): Promise<HeadersInit> {
    // Ensure token is fresh
    await authService.autoRefreshIfNeeded();

    const accessToken = authService.getAccessToken();
    if (!accessToken) {
      throw new Error('Not authenticated');
    }

    return {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${accessToken}`,
    };
  }

  /**
   * Get my profile
   */
  async getProfile(): Promise<ProfileResponse> {
    const apiBase = getApiBaseUrl();

    try {
      console.log('[Profile] Fetching profile...');

      const headers = await this.getAuthHeaders();
      const response = await fetch(`${apiBase}/auth-service/api/profile/me`, {
        method: 'GET',
        headers,
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || `Failed to fetch profile: ${response.status}`);
      }

      const result = await response.json();
      const data: ProfileResponse = result.data || result;

      console.log('Profile fetched successfully');
      return data;
    } catch (error) {
      console.error('Failed to fetch profile:', error);
      throw error;
    }
  }

  /**
   * Update profile
   */
  async updateProfile(request: UpdateProfileRequest): Promise<ProfileResponse> {
    const apiBase = getApiBaseUrl();

    try {
      console.log('[Profile] Updating profile...');

      const headers = await this.getAuthHeaders();
      const response = await fetch(`${apiBase}/auth-service/api/profile`, {
        method: 'PATCH',
        headers,
        body: JSON.stringify(request),
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || `Failed to update profile: ${response.status}`);
      }

      const result = await response.json();
      const data: ProfileResponse = result.data || result;

      console.log('Profile updated successfully');
      return data;
    } catch (error) {
      console.error('Failed to update profile:', error);
      throw error;
    }
  }

  /**
   * Change password
   */
  async changePassword(request: ChangePasswordRequest): Promise<void> {
    const apiBase = getApiBaseUrl();

    try {
      console.log('[Profile] Changing password...');

      const headers = await this.getAuthHeaders();
      const response = await fetch(`${apiBase}/auth-service/api/profile/password`, {
        method: 'POST',
        headers,
        body: JSON.stringify(request),
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || `Failed to change password: ${response.status}`);
      }

      console.log('Password changed successfully');
    } catch (error) {
      console.error('Failed to change password:', error);
      throw error;
    }
  }

  /**
   * Delete account (soft delete)
   */
  async deleteAccount(request: DeleteAccountRequest): Promise<void> {
    const apiBase = getApiBaseUrl();

    try {
      console.log('[Profile] Deleting account...');

      const headers = await this.getAuthHeaders();
      const response = await fetch(`${apiBase}/auth-service/api/profile/account`, {
        method: 'DELETE',
        headers,
        body: JSON.stringify(request),
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || `Failed to delete account: ${response.status}`);
      }

      // Clear tokens after successful account deletion
      authService.clearTokens();

      console.log('Account deleted successfully');
    } catch (error) {
      console.error('Failed to delete account:', error);
      throw error;
    }
  }
}

// ====================================================================
// Singleton instance
// ====================================================================

export const profileService = new ProfileServiceClass();

// ====================================================================
// Public API
// ====================================================================

export async function getProfile(): Promise<ProfileResponse> {
  return profileService.getProfile();
}

export async function updateProfile(request: UpdateProfileRequest): Promise<ProfileResponse> {
  return profileService.updateProfile(request);
}

export async function changePassword(request: ChangePasswordRequest): Promise<void> {
  return profileService.changePassword(request);
}

export async function deleteAccount(request: DeleteAccountRequest): Promise<void> {
  return profileService.deleteAccount(request);
}

// Default export
export default profileService;
