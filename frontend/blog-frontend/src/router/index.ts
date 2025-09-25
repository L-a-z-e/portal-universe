import { createRouter, createWebHistory } from "vue-router";

import PostListPage from '../views/PostListPage.vue';
import PostDetailPage from '../views/PostDetailPage.vue';
import PostWritePage from '../views/PostWritePage.vue';

const routes = [
  {
    path: '',
    name: 'PostList',
    component: PostListPage
  },
  {
    path: '/:postId',
    name: 'PostDetail',
    component: PostDetailPage,
    props: true
  },
  {
    path: '/write',
    name: 'PostWrite',
    component: PostWritePage
  }
];

const router = createRouter({
  history: createWebHistory(), // base 경로를 /blog로 설정
  routes
});

export default router;