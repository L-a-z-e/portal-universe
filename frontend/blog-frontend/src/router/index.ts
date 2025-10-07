import {createMemoryHistory, createRouter, type Router} from "vue-router";

import PostListPage from '../views/PostListPage.vue';
import PostDetailPage from '../views/PostDetailPage.vue';
import PostWritePage from '../views/PostWritePage.vue';
import PostEditPage from '../views/PostEditPage.vue';

const routes = [
  {
    path: '/',
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
  },
  {
    path: '/edit/:postId',
    name: 'PostEdit',
    component: PostEditPage,
    props: true
  }
];

export const createBlogRouter = (): Router => {
  return createRouter({
    history: createMemoryHistory(),
    routes
  });
}