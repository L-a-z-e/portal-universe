import {createApp} from 'vue';
import App from './App.vue';
import {createBlogRouter} from "./router";

export function mountBlogApp(el: HTMLElement) {
  if (!el) return () => {};
  const app = createApp(App);
  const router = createBlogRouter();
  app.use(router);
  app.mount(el);

  return {
    router,
    unmount: () => {
      app.unmount();
      el.innerHTML = '';
    }
  };
}