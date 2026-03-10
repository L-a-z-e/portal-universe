// portal-shell/src/services/profileService.ts

import { authService } from './authService';
import { throwIfNotOk } from './fetchUtils';

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

function getApiBaseUrl(): string {
  return import.meta.env.VITE_API_BASE_URL || window.location.origin;
}

class ProfileServiceClass {
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

  async getProfile(): Promise<ProfileResponse> {
    const apiBase = getApiBaseUrl();

    try {

      const headers = await this.getAuthHeaders();
      const response = await fetch(`${apiBase}/auth-service/api/v1/profile/me`, {
        method: 'GET',
        headers,
      });

      await throwIfNotOk(response, 'Failed to fetch profile');

      const result = await response.json();
      const data: ProfileResponse = result.data || result;

      return data;
    } catch (error) {
      console.error('Failed to fetch profile:', error);
      throw error;
    }
  }

  async updateProfile(request: UpdateProfileRequest): Promise<ProfileResponse> {
    const apiBase = getApiBaseUrl();

    try {

      const headers = await this.getAuthHeaders();
      const response = await fetch(`${apiBase}/auth-service/api/v1/profile`, {
        method: 'PATCH',
        headers,
        body: JSON.stringify(request),
      });

      await throwIfNotOk(response, 'Failed to update profile');

      const result = await response.json();
      const data: ProfileResponse = result.data || result;

      return data;
    } catch (error) {
      console.error('Failed to update profile:', error);
      throw error;
    }
  }

  async changePassword(request: ChangePasswordRequest): Promise<void> {
    const apiBase = getApiBaseUrl();

    try {

      const headers = await this.getAuthHeaders();
      const response = await fetch(`${apiBase}/auth-service/api/v1/profile/password`, {
        method: 'POST',
        headers,
        body: JSON.stringify(request),
      });

      await throwIfNotOk(response, 'Failed to change password');

    } catch (error) {
      console.error('Failed to change password:', error);
      throw error;
    }
  }

  async deleteAccount(request: DeleteAccountRequest): Promise<void> {
    const apiBase = getApiBaseUrl();

    try {

      const headers = await this.getAuthHeaders();
      const response = await fetch(`${apiBase}/auth-service/api/v1/profile/account`, {
        method: 'DELETE',
        headers,
        body: JSON.stringify(request),
      });

      await throwIfNotOk(response, 'Failed to delete account');

      // Clear tokens after successful account deletion
      authService.clearTokens();

    } catch (error) {
      console.error('Failed to delete account:', error);
      throw error;
    }
  }
}

export const profileService = new ProfileServiceClass();

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
