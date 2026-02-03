import http from 'k6/http';
import { check } from 'k6';
import { login, authHeaders } from '../lib/auth.js';
import { config } from '../lib/config.js';

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
    http_req_duration: ['p(99)<500'],
    // Error rate threshold is intentionally relaxed:
    // most requests will fail with SOLD_OUT after coupon limit is reached
  },
};

export function setup() {
  const token = login();
  if (!token) throw new Error('Setup failed: cannot login');
  return { token, couponId: __ENV.COUPON_ID || '1' };
}

export default function (data) {
  const params = authHeaders(data.token);

  // Gateway: /api/v1/shopping/** → shopping-service
  const res = http.post(
    `${config.BASE_URL}/api/v1/shopping/coupons/${data.couponId}/issue`,
    null,
    Object.assign({}, params, { tags: { name: 'coupon_issue' } })
  );

  check(res, {
    'response received': (r) => r.status === 200 || r.status === 400 || r.status === 409,
  });
}
