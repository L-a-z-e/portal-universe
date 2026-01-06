// portal_shell/src/types/federation.d.ts

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