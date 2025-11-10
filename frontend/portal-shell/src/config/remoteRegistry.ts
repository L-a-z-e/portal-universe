// portal_shell/src/config/remoteRegistry.ts

export type RemoteConfig = {
  name: string;              // 표시 이름 (예: 'Blog')
  key: string;               // federation key (예: 'blog_remote')
  url: string;               // remoteEntry.js URL
  module: string;            // 로드할 모듈 경로 (예: 'blog_remote/bootstrap')
  mountFn: string;           // mount 함수 이름 (예: 'mountBlogApp')
  basePath: string;          // 라우팅 base path (예: '/blog')
  icon?: string;             // 아이콘 (선택)
  description?: string;      // 설명 (선택)
};

// ✅ 환경 타입 정의
type EnvironmentMode = 'dev' | 'docker' | 'k8s';

// ✅ Record 타입을 엄격하게
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

// ✅ 환경 변수를 EnvironmentMode로 변환
function getEnvironmentMode(): EnvironmentMode {
  const mode = import.meta.env.VITE_PROFILE;

  // 유효한 mode인지 체크
  if (mode === 'dev' || mode === 'docker' || mode === 'k8s') {
    return mode;
  }

  // 기본값
  console.warn(`⚠️ Unknown environment mode: ${mode}, falling back to 'dev'`);
  return 'dev';
}

export function getRemoteConfigs(): RemoteConfig[] {
  const mode = getEnvironmentMode();
  return remoteConfigs[mode];
}

export function getRemoteConfig(basePath: string): RemoteConfig | undefined {
  return getRemoteConfigs().find(r => r.basePath === basePath);
}