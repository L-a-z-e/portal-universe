/**
 * @file remoteRegistry.ts
 * @description 마이크로 프론트엔드(Remote) 애플리케이션들의 설정을 중앙에서 관리합니다.
 * 환경(dev, docker, k8s)에 따라 다른 URL을 제공하여, 환경 간 전환을 용이하게 합니다.
 */

/**
 * Remote 애플리케이션 하나의 설정 정보를 담는 타입입니다.
 */
export type RemoteConfig = {
  name: string;              // UI에 표시될 이름 (예: 'Blog')
  key: string;               // Federation 설정에서 사용할 고유 키 (예: 'blog_remote')
  url: string;               // remoteEntry.js 파일의 전체 URL
  module: string;            // 로드할 모듈의 경로 (일반적으로 'key/bootstrap')
  mountFn: string;           // 해당 모듈에서 내보내는 마운트 함수의 이름
  basePath: string;          // 이 Remote 앱이 담당할 라우팅 경로의 접두사 (예: '/blog')
  icon?: string;             // UI에 표시될 아이콘 (선택 사항)
  description?: string;      // UI에 표시될 설명 (선택 사항)
};

// 환경 모드 타입을 정의합니다.
type EnvironmentMode = 'dev' | 'docker' | 'k8s';

// 각 환경별 Remote 설정 목록입니다.
const remoteConfigs: Record<EnvironmentMode, RemoteConfig[]> = {
  dev: [
    {
      name: 'Blog',
      key: 'blog_remote',
      url: 'http://localhost:30001/assets/remoteEntry.js',
      module: 'blog_remote/bootstrap',
      mountFn: 'mountBlogApp',
      basePath: '/blog',
      icon: '📝',
      description: '블로그 서비스'
    },
  ],
  docker: [
    {
      name: 'Blog',
      key: 'blog_remote',
      url: 'http://blog-frontend:30001/assets/remoteEntry.js',
      module: 'blog_remote/bootstrap',
      mountFn: 'mountBlogApp',
      basePath: '/blog',
      icon: '📝',
      description: '블로그 서비스'
    },
  ],
  k8s: [
    {
      name: 'Blog',
      key: 'blog_remote',
      url: 'http://portal-universe:8080/blog-frontend/assets/remoteEntry.js',
      module: 'blog_remote/bootstrap',
      mountFn: 'mountBlogApp',
      basePath: '/blog',
      icon: '📝',
      description: '블로그 서비스'
    },
  ]
};

/**
 * Vite 환경 변수(VITE_PROFILE)를 읽어 현재 환경 모드를 반환합니다.
 * @returns {EnvironmentMode} 현재 환경 모드 ('dev', 'docker', 'k8s')
 */
function getEnvironmentMode(): EnvironmentMode {
  const mode = import.meta.env.VITE_PROFILE;

  if (mode === 'dev' || mode === 'docker' || mode === 'k8s') {
    return mode;
  }

  console.warn(`⚠️ Unknown environment mode: ${mode}, falling back to 'dev'`);
  return 'dev'; // 기본값으로 'dev'를 반환
}

/**
 * 현재 환경에 맞는 모든 Remote 설정 목록을 가져옵니다.
 * @returns {RemoteConfig[]} Remote 설정 배열
 */
export function getRemoteConfigs(): RemoteConfig[] {
  const mode = getEnvironmentMode();
  return remoteConfigs[mode];
}

/**
 * basePath를 기준으로 특정 Remote 설정을 찾습니다.
 * @param {string} basePath - 찾고자 하는 Remote의 라우팅 경로 접두사
 * @returns {RemoteConfig | undefined} 해당하는 Remote 설정 객체 또는 undefined
 */
export function getRemoteConfig(basePath: string): RemoteConfig | undefined {
  return getRemoteConfigs().find(r => r.basePath === basePath);
}
