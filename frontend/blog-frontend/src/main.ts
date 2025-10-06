import './style.css'
import { mountBlogApp } from './bootstrap';

const appElement = document.querySelector('#app') as HTMLElement | null;

if (appElement) {
  mountBlogApp(appElement, {
    onNavigate: (path) => {
      console.log(`Standalone navigation: ${path}`);
    }
  });
}