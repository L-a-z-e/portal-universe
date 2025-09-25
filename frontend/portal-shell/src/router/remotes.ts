import { mountWrapper } from '../utils/mountWrapper';
import { mountBlogApp } from 'blog_remote/bootstrap'; // remote expose 필요

export const remoteRoutes = [
  {
    path: '/blog/:pathMatch(.*)*',
    name: 'blog',
    component: mountWrapper(mountBlogApp),
  },
];