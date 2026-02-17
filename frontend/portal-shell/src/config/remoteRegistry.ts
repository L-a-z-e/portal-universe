// portal_shell/src/config/remoteRegistry.ts

export type RemoteConfig = {
  name: string;              // 표시 이름
  key: string;               // federation key
  url: string;               // remoteEntry.js URL
  module: string;            // 로드할 모듈 경로
  mountFn: string;           // mount 함수 이름
  basePath: string;          // 라우팅 base path
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
      key: 'blog',
      url: 'http://localhost:30001/assets/remoteEntry.js',
      module: 'blog/bootstrap',
      mountFn: 'mountBlogApp',
      basePath: '/blog',
      icon: 'article',
      description: '블로그 서비스'
    },
    {
      name: 'Shopping',
      key: 'shopping',
      url: 'http://localhost:30002/assets/remoteEntry.js',
      module: 'shopping/bootstrap',
      mountFn: 'mountShoppingApp',
      basePath: '/shopping',
      icon: 'shopping_cart',
      description: '쇼핑 서비스'
    },
    {
      name: 'Prism',
      key: 'prism',
      url: 'http://localhost:30003/assets/remoteEntry.js',
      module: 'prism/bootstrap',
      mountFn: 'mountPrismApp',
      basePath: '/prism',
      icon: 'smart_toy',
      description: 'AI 에이전트 오케스트레이션'
    },
    {
      name: 'Drive',
      key: 'drive',
      url: 'http://localhost:30005/assets/remoteEntry.js',
      module: 'drive/bootstrap',
      mountFn: 'mountDriveApp',
      basePath: '/drive',
      icon: 'cloud_upload',
      description: '파일 관리'
    },
    {
      name: 'Admin',
      key: 'admin',
      url: 'http://localhost:30004/assets/remoteEntry.js',
      module: 'admin/bootstrap',
      mountFn: 'mountAdminApp',
      basePath: '/admin',
      icon: 'admin_panel_settings',
      description: '관리 대시보드'
    },
    {
      name: 'Seller',
      key: 'seller',
      url: 'http://localhost:30006/assets/remoteEntry.js',
      module: 'seller/bootstrap',
      mountFn: 'mountSellerApp',
      basePath: '/seller',
      icon: 'storefront',
      description: '쇼핑몰 판매자 관리'
    },
  ],
  docker: [
    {
      name: 'Blog',
      key: 'blog',
      url: import.meta.env.VITE_BLOG_REMOTE_URL || 'http://localhost:30001/assets/remoteEntry.js',
      module: 'blog/bootstrap',
      mountFn: 'mountBlogApp',
      basePath: '/blog',
      icon: 'article',
      description: '블로그 서비스'
    },
    {
      name: 'Shopping',
      key: 'shopping',
      url: import.meta.env.VITE_SHOPPING_REMOTE_URL || 'http://localhost:30002/assets/remoteEntry.js',
      module: 'shopping/bootstrap',
      mountFn: 'mountShoppingApp',
      basePath: '/shopping',
      icon: 'shopping_cart',
      description: '쇼핑 서비스'
    },
    {
      name: 'Prism',
      key: 'prism',
      url: import.meta.env.VITE_PRISM_REMOTE_URL || 'http://localhost:30003/assets/remoteEntry.js',
      module: 'prism/bootstrap',
      mountFn: 'mountPrismApp',
      basePath: '/prism',
      icon: 'smart_toy',
      description: 'AI 에이전트 오케스트레이션'
    },
    {
      name: 'Drive',
      key: 'drive',
      url: import.meta.env.VITE_DRIVE_REMOTE_URL || 'http://localhost:30005/assets/remoteEntry.js',
      module: 'drive/bootstrap',
      mountFn: 'mountDriveApp',
      basePath: '/drive',
      icon: 'cloud_upload',
      description: '파일 관리'
    },
    {
      name: 'Admin',
      key: 'admin',
      url: import.meta.env.VITE_ADMIN_REMOTE_URL || 'http://localhost:30004/assets/remoteEntry.js',
      module: 'admin/bootstrap',
      mountFn: 'mountAdminApp',
      basePath: '/admin',
      icon: 'admin_panel_settings',
      description: '관리 대시보드'
    },
    {
      name: 'Seller',
      key: 'seller',
      url: import.meta.env.VITE_SELLER_REMOTE_URL || 'http://localhost:30006/assets/remoteEntry.js',
      module: 'seller/bootstrap',
      mountFn: 'mountSellerApp',
      basePath: '/seller',
      icon: 'storefront',
      description: '쇼핑몰 판매자 관리'
    },
  ],
  k8s: [
    {
      name: 'Blog',
      key: 'blog',
      url: import.meta.env.VITE_BLOG_REMOTE_URL,
      module: 'blog/bootstrap',
      mountFn: 'mountBlogApp',
      basePath: '/blog',
      icon: 'article',
      description: '블로그 서비스'
    },
    {
      name: 'Shopping',
      key: 'shopping',
      url: import.meta.env.VITE_SHOPPING_REMOTE_URL,
      module: 'shopping/bootstrap',
      mountFn: 'mountShoppingApp',
      basePath: '/shopping',
      icon: 'shopping_cart',
      description: '쇼핑 서비스'
    },
    {
      name: 'Prism',
      key: 'prism',
      url: import.meta.env.VITE_PRISM_REMOTE_URL,
      module: 'prism/bootstrap',
      mountFn: 'mountPrismApp',
      basePath: '/prism',
      icon: 'smart_toy',
      description: 'AI 에이전트 오케스트레이션'
    },
    {
      name: 'Drive',
      key: 'drive',
      url: import.meta.env.VITE_DRIVE_REMOTE_URL,
      module: 'drive/bootstrap',
      mountFn: 'mountDriveApp',
      basePath: '/drive',
      icon: 'cloud_upload',
      description: '파일 관리'
    },
    {
      name: 'Admin',
      key: 'admin',
      url: import.meta.env.VITE_ADMIN_REMOTE_URL,
      module: 'admin/bootstrap',
      mountFn: 'mountAdminApp',
      basePath: '/admin',
      icon: 'admin_panel_settings',
      description: '관리 대시보드'
    },
    {
      name: 'Seller',
      key: 'seller',
      url: import.meta.env.VITE_SELLER_REMOTE_URL,
      module: 'seller/bootstrap',
      mountFn: 'mountSellerApp',
      basePath: '/seller',
      icon: 'storefront',
      description: '쇼핑몰 판매자 관리'
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