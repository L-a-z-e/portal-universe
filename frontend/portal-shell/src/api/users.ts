import apiClient from './apiClient';

export interface SignupRequest {
  email: string;
  password?: string;
  nickname: string;
  realName?: string;
  marketingAgree: boolean;
}

const BASE_PATH = '/api/users';

export function signup(payload: SignupRequest) {
  return apiClient.post(`${BASE_PATH}/signup`, payload);
}