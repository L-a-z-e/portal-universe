// frontend/blog-frontend/src/config/assets.ts

/**
 * Static Asset URL 관리
 * 환경별로 다른 경로를 반환
 */

// 환경별 base URL
const getAssetBaseUrl = (): string => {
  const env = import.meta.env.MODE;

  switch (env) {
    case 'development':
      // 로컬 개발: blog-frontend 자체 서버
      return 'http://localhost:30001';

    case 'docker':
    case 'kubernetes':
    case 'production':
      // Docker/K8s/운영: portal-shell을 통한 프록시
      return '/remotes/blog';

    default:
      return '/remotes/blog';
  }
};

export const ASSET_BASE_URL = getAssetBaseUrl();

/**
 * Asset URL 생성 헬퍼
 */
export const getAssetUrl = (path: string): string => {
  // 이미 절대 URL이면 그대로 반환
  if (path.startsWith('http://') || path.startsWith('https://')) {
    return path;
  }

  // 슬래시로 시작하지 않으면 추가
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;

  return `${ASSET_BASE_URL}${normalizedPath}`;
};

/**
 * 기본 썸네일 타입별 경로
 */
export const DEFAULT_THUMBNAILS = {
  write: getAssetUrl('/default-thumbnail-write.png'),
  travel: getAssetUrl('/default-thumbnail-travel.png'),
  tech: getAssetUrl('/default-thumbnail-tech.png'),
} as const;

export type ThumbnailType = keyof typeof DEFAULT_THUMBNAILS;