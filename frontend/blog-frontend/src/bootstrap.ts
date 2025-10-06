import {createApp} from 'vue';
import App from './App.vue';
import {createBlogRouter} from "./router";

export type MountOptions = {
  initialPath?: string;
  onNavigate?: (path: string) => void;
}

export function mountBlogApp(el: HTMLElement, options: MountOptions = {}) {
  if (!el) return () => {};

  const { initialPath, onNavigate } = options;
  const app = createApp(App);
  const router = createBlogRouter();
  app.use(router);

  if (initialPath) {
    router.push(initialPath).catch(err => console.error('Initial navigation failed:', err));
  }

  router.afterEach((to, from) => {
    if (to.path !== from.path) {
      onNavigate?.(to.path);
    }
  });

  app.mount(el);

  return {
    router,
    onParentNavigate: (path: string) => {
      if (router.currentRoute.value.path !== path) {
        router.push(path).catch(err => console.error('Parent navigation failed:', err));
      }
    },
    unmount: () => {
      app.unmount();
      el.innerHTML = '';
    }
  };
}