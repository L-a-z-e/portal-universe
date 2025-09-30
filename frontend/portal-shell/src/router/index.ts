import { createRouter, createWebHistory } from 'vue-router';
import RemoteWrapper from '../components/RemoteWrapper.vue';
import HomePage from "../views/HomePage.vue";
import {mountBlogApp} from "blog_remote/bootstrap";

const routes = [
  { path: '/', name: 'Home', component: HomePage },
  { path: '/blog/:pathMatch(.*)*',
    name: 'blog',
    component: RemoteWrapper,
    props: {
      mountFn: mountBlogApp,
      basePath: '/blog'
    }
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

export default router;