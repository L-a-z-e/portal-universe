import http from 'k6/http';
import { sleep } from 'k6';
import { login, authHeaders } from '../lib/auth.js';
import { config } from '../lib/config.js';
import { checkApiResponse } from '../lib/checks.js';

const SEARCH_TERMS = ['노트북', '스마트폰', '이어폰', '키보드', '마우스',
  '모니터', '태블릿', '카메라', '프린터', '스피커'];

export const options = {
  scenarios: {
    search_load: {
      executor: 'ramping-arrival-rate',
      startRate: 50,
      timeUnit: '1s',
      stages: [
        { duration: '3m', target: 200 },
        { duration: '10m', target: 200 },
        { duration: '2m', target: 0 },
      ],
      preAllocatedVUs: 100,
      maxVUs: 300,
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<100'],
    http_req_failed: ['rate<0.01'],
  },
};

export function setup() {
  const token = login();
  if (!token) throw new Error('Setup failed: cannot login');
  return { token };
}

export default function (data) {
  const params = authHeaders(data.token);
  const term = SEARCH_TERMS[Math.floor(Math.random() * SEARCH_TERMS.length)];

  // Gateway: /api/v1/shopping/** → shopping-service
  const res = http.get(
    `${config.BASE_URL}/api/v1/shopping/products/search?keyword=${encodeURIComponent(term)}&page=0&size=20`,
    Object.assign({}, params, { tags: { name: 'product_search' } })
  );
  checkApiResponse(res, 'product_search');

  sleep(0.2);
}
