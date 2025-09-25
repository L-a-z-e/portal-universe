import { createRouter, createWebHistory } from "vue-router";
import { defineAsyncComponent } from "vue";

const BlogApp = defineAsyncComponent(() => import('blog_remote/BlogApp'));

const routes = [
  {
    path: "/blog",
    name: "blog",
    component: BlogApp
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
});

export default router;