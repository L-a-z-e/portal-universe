import http from 'k6/http';
import { check } from 'k6';
import { login, loginBulk, getTokenForVU, authHeaders } from '../lib/auth.js';
import { config } from '../lib/config.js';

const USE_BULK = __ENV.USE_BULK === 'true';

export const options = {
  scenarios: {
    coupon_spike: {
      executor: 'shared-iterations',
      vus: 500,
      iterations: 500,
      maxDuration: '30s',
    },
  },
  thresholds: {
    'http_req_duration{name:coupon_issue}': ['p(99)<500'],
    'http_req_duration{name:login}': ['p(95)<5000'],
    // Error rate threshold is intentionally relaxed:
    // most requests will fail with SOLD_OUT after coupon limit is reached
  },
};

export function setup() {
  if (USE_BULK) {
    const tokens = loginBulk();
    if (tokens.length === 0) throw new Error('Setup failed: bulk login returned 0 tokens');
    return { tokens, couponId: __ENV.COUPON_ID || '1' };
  }
  const token = login();
  if (!token) throw new Error('Setup failed: cannot login');
  return { tokens: [token], couponId: __ENV.COUPON_ID || '1' };
}

export default function (data) {
  const token = USE_BULK ? getTokenForVU(data.tokens) : data.tokens[0];
  const params = authHeaders(token);

  // Gateway: /api/v1/shopping/** → shopping-service
  const res = http.post(
    `${config.BASE_URL}/api/v1/shopping/coupons/${data.couponId}/issue`,
    null,
    Object.assign({}, params, { tags: { name: 'coupon_issue' } })
  );

  check(res, {
    'coupon_issue status valid': (r) => [200, 400, 409, 429, 503].includes(r.status),
    'coupon_issue success or expected error': (r) => r.status === 200 || r.status === 409,
  });
}
