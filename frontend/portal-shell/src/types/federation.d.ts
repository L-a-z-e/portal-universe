// portal_shell/src/types/federation.d.ts

// Window 인터페이스 확장 - Remote 앱에서 사용할 전역 토큰
declare global {
  interface Window {
    __PORTAL_ACCESS_TOKEN__?: string;
  }
}

declare module "blog/bootstrap" {
  export type MountOptions = {
    initialPath?: string;
    onNavigate?: (path: string) => void;
  };

  export function mountBlogApp(
    el: HTMLElement,
    options?: MountOptions
  ): {
    router: import('vue-router').Router;
    onParentNavigate: (path: string) => void;
    unmount: () => void;
  };
}

export {};