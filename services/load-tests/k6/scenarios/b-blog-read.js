import http from 'k6/http';
import { sleep } from 'k6';
import { login, loginBulk, getTokenForVU, authHeaders } from '../lib/auth.js';
import { config } from '../lib/config.js';
import { checkApiResponse } from '../lib/checks.js';

const USE_BULK = __ENV.USE_BULK === 'true';

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
    http_req_failed: ['rate<0.01'],
    'http_req_duration{name:post_list}': ['p(95)<80'],
    'http_req_duration{name:post_detail}': ['p(95)<50'],
    'http_req_duration{name:login}': ['p(95)<5000'],
  },
};

export function setup() {
  if (USE_BULK) {
    const tokens = loginBulk();
    if (tokens.length === 0) throw new Error('Setup failed: bulk login returned 0 tokens');
    return { tokens };
  }
  const token = login();
  if (!token) throw new Error('Setup failed: cannot login');
  return { tokens: [token] };
}

export default function (data) {
  const token = USE_BULK ? getTokenForVU(data.tokens) : data.tokens[0];
  const params = authHeaders(token);
  const base = config.BASE_URL;

  // Gateway: /api/v1/blog/** → blog-service
  // 매 iteration: 목록 조회 → 70% 목록만, 30% 상세까지
  const page = Math.floor(Math.random() * 10) + 1;
  const list = http.get(
    `${base}/api/v1/blog/posts?page=${page}&size=20`,
    Object.assign({}, params, { tags: { name: 'post_list' } })
  );
  checkApiResponse(list, 'post_list');

  // 30% detail — 목록에서 실제 ObjectId 추출
  if (Math.random() < 0.3) {
    let postId;
    try {
      const items = list.json('data.items');
      if (items && items.length > 0) {
        postId = items[Math.floor(Math.random() * items.length)].id;
      }
    } catch (_) { /* ignore parse errors */ }

    if (postId) {
      const detail = http.get(
        `${base}/api/v1/blog/posts/${postId}`,
        Object.assign({}, params, { tags: { name: 'post_detail' } })
      );
      checkApiResponse(detail, 'post_detail');
    }
  }

  sleep(0.1);
}
