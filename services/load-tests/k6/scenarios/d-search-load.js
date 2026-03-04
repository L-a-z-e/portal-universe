import http from 'k6/http';
import { sleep } from 'k6';
import { login, loginBulk, getTokenForVU, authHeaders } from '../lib/auth.js';
import { config } from '../lib/config.js';
import { checkApiResponse } from '../lib/checks.js';

const USE_BULK = __ENV.USE_BULK === 'true';

const SEARCH_TERMS = ['Electronics', 'Premium', 'Item', 'quality', 'product',
  'high', 'description', 'price', 'stock', 'special'];

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
    http_req_failed: ['rate<0.01'],
    'http_req_duration{name:product_search}': ['p(95)<100'],
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
  const term = SEARCH_TERMS[Math.floor(Math.random() * SEARCH_TERMS.length)];

  // Gateway: /api/v1/shopping/** → shopping-service
  const res = http.get(
    `${config.BASE_URL}/api/v1/shopping/search/products?keyword=${encodeURIComponent(term)}&page=0&size=20`,
    Object.assign({}, params, { tags: { name: 'product_search' } })
  );
  checkApiResponse(res, 'product_search');

  sleep(0.2);
}
