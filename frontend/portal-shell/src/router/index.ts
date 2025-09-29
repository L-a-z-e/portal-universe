import { createRouter, createWebHistory } from 'vue-router';
import { remoteRoutes } from './remotes';
import HomePage from "../views/HomePage.vue";

const routes = [
  { path: '/', name: 'Home', component: HomePage },
  ...remoteRoutes,
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

export default router;