import apiClient from './apiClient';

const BASE_PATH = '/api/v1/auth/password-reset';

export function requestPasswordReset(email: string) {
  return apiClient.post(`${BASE_PATH}/request`, { email });
}

export async function validateResetToken(token: string): Promise<boolean> {
  try {
    await apiClient.get(`${BASE_PATH}/validate`, { params: { token } });
    return true;
  } catch {
    return false;
  }
}

export function resetPassword(token: string, newPassword: string, confirmPassword: string) {
  return apiClient.post(`${BASE_PATH}/reset`, { token, newPassword, confirmPassword });
}
