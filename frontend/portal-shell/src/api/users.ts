import apiClient from './apiClient';

export interface SignupRequest {
  email: string;
  password?: string;
  nickname: string;
  realName?: string;
  marketingAgree: boolean;
}

export interface PasswordPolicyResponse {
  minLength: number;
  maxLength: number;
  requirements: string[];
}

const BASE_PATH = '/api/v1/users';
const AUTH_PATH = '/api/v1/auth';

export function signup(payload: SignupRequest) {
  return apiClient.post(`${BASE_PATH}/signup`, payload);
}

export async function getPasswordPolicy(): Promise<PasswordPolicyResponse> {
  const res = await apiClient.get<{ data: PasswordPolicyResponse }>(`${AUTH_PATH}/password-policy`);
  return res.data.data;
}