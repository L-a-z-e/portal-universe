import { createApp } from 'vue';
import App from './App.vue';
import router from './router';

export function mountBlogApp(el: HTMLElement) {
  if (!el) return () => {};
  const app = createApp(App);
  app.use(router);
  app.mount(el);

  return () => {
    app.unmount();
    el.innerHTML = '';
  };
}