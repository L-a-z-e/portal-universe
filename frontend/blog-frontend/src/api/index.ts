import axios from 'axios';

/**
 * @file api/index.ts
 * @description 백엔드 API와 통신하기 위한 Axios 인스턴스를 생성하고 설정합니다.
 */

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL, // .env 파일에 정의된 API 기본 URL
});

/**
 * Axios 요청 인터셉터(interceptor)입니다.
 * 모든 API 요청이 보내지기 전에, localStorage에서 JWT 토큰을 읽어
 * 'Authorization' 헤더에 추가하는 역할을 합니다.
 * 이를 통해 Standalone 모드에서도 인증이 필요한 API를 호출할 수 있습니다.
 */
apiClient.interceptors.request.use(
  (config) => {
    // oidc-client-ts는 특정 형식의 키로 localStorage에 사용자 정보를 저장합니다.
    const oidcStorage = localStorage.getItem(`oidc.user:${import.meta.env.VITE_OIDC_AUTHORITY}:${import.meta.env.VITE_OIDC_CLIENT_ID}`);

    if (oidcStorage) {
      try {
        const user = JSON.parse(oidcStorage);
        const token = user.access_token;

        if (token) {
          config.headers.Authorization = `Bearer ${token}`;
        }
      } catch (e) {
        console.error('Failed to parse OIDC user from localStorage', e);
      }
    }

    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

export default apiClient;
