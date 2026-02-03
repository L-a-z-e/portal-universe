import http from 'k6/http';
import { check } from 'k6';
import { login, authHeaders } from '../lib/auth.js';
import { config } from '../lib/config.js';

const PRODUCT_ID = __ENV.PRODUCT_ID || '1';

export const options = {
  scenarios: {
    // Phase 1: Warm the cache
    cache_warm: {
      executor: 'shared-iterations',
      vus: 1,
      iterations: 1,
      maxDuration: '10s',
      exec: 'warmCache',
      startTime: '0s',
    },
    // Phase 2: Simultaneous requests after cache expiry
    thundering_herd: {
      executor: 'shared-iterations',
      vus: 200,
      iterations: 200,
      maxDuration: '10s',
      exec: 'thunderingHerd',
      startTime: '15s',
    },
  },
  thresholds: {
    'http_req_duration{scenario:thundering_herd}': ['p(95)<500'],
    'http_req_failed{scenario:thundering_herd}': ['rate<0.05'],
  },
};

export function setup() {
  const token = login();
  if (!token) throw new Error('Setup failed: cannot login');
  return { token };
}

export function warmCache(data) {
  const params = authHeaders(data.token);
  // Gateway: /api/v1/shopping/** → shopping-service
  const res = http.get(
    `${config.BASE_URL}/api/v1/shopping/products/${PRODUCT_ID}`,
    params
  );
  check(res, { 'cache warmed': (r) => r.status === 200 });
}

export function thunderingHerd(data) {
  const params = authHeaders(data.token);
  // Gateway: /api/v1/shopping/** → shopping-service
  const res = http.get(
    `${config.BASE_URL}/api/v1/shopping/products/${PRODUCT_ID}`,
    Object.assign({}, params, { tags: { name: 'thundering_herd' } })
  );
  check(res, {
    'response ok': (r) => r.status === 200,
  });
}
