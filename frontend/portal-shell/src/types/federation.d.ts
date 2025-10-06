declare module "blog_remote/bootstrap" {
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