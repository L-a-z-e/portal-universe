import http from 'k6/http';
import { sleep } from 'k6';
import { login, authHeaders } from '../lib/auth.js';
import { config } from '../lib/config.js';
import { checkApiResponse } from '../lib/checks.js';

export const options = {
  scenarios: {
    blog_read: {
      executor: 'constant-arrival-rate',
      rate: 200,
      timeUnit: '1s',
      duration: '10m',
      preAllocatedVUs: 100,
      maxVUs: 300,
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<100'],
    http_req_failed: ['rate<0.01'],
    'http_req_duration{name:post_list}': ['p(95)<80'],
    'http_req_duration{name:post_detail}': ['p(95)<50'],
  },
};

export function setup() {
  const token = login();
  if (!token) throw new Error('Setup failed: cannot login');
  return { token };
}

export default function (data) {
  const params = authHeaders(data.token);
  const base = config.BASE_URL;

  // 70% list, 30% detail (realistic traffic pattern)
  // Gateway: /api/v1/blog/** → blog-service
  if (Math.random() < 0.7) {
    const page = Math.floor(Math.random() * 10);
    const list = http.get(
      `${base}/api/v1/blog/posts?page=${page}&size=20`,
      Object.assign({}, params, { tags: { name: 'post_list' } })
    );
    checkApiResponse(list, 'post_list');
  } else {
    const postId = Math.floor(Math.random() * 100) + 1;
    http.get(
      `${base}/api/v1/blog/posts/${postId}`,
      Object.assign({}, params, { tags: { name: 'post_detail' } })
    );
  }

  sleep(0.1);
}
