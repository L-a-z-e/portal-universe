import { createRouter, createWebHistory } from 'vue-router';
import App from '../App.vue';
import { remoteRoutes } from './remotes';

const routes = [
  { path: '/', name: 'Home', component: App },
  ...remoteRoutes,
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

export default router;