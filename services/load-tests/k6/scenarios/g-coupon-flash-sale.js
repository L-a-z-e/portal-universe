import { check } from 'k6';
import { Counter } from 'k6/metrics';
import { login, loginBulk, getTokenForVU, authHeaders } from '../lib/auth.js';
import { config } from '../lib/config.js';
import { issueCoupon } from '../lib/shopping-helpers.js';

const USE_BULK = __ENV.USE_BULK === 'true';
const FLASH_50_ID = __ENV.FLASH_50_ID || '26';  // FLASH_50 = 26th coupon
const FLASH_100_ID = __ENV.FLASH_100_ID || '27'; // FLASH_100 = 27th coupon

// Custom metrics
const couponIssued = new Counter('coupon_issued');
const couponSoldOut = new Counter('coupon_sold_out');
const couponDuplicate = new Counter('coupon_duplicate');
const couponRateLimited = new Counter('coupon_rate_limited');

export const options = {
  scenarios: {
    flash_50: {
      executor: 'shared-iterations',
      vus: 500,
      iterations: 500,
      maxDuration: '30s',
      env: { COUPON_TARGET: 'flash_50' },
    },
    flash_100: {
      executor: 'shared-iterations',
      vus: 500,
      iterations: 500,
      maxDuration: '30s',
      startTime: '35s',
      env: { COUPON_TARGET: 'flash_100' },
    },
  },
  thresholds: {
    'http_req_duration{name:coupon_issue}': ['p(99)<500'],
    'http_req_duration{name:login}': ['p(95)<5000'],
  },
};

export function setup() {
  let tokens;
  if (USE_BULK) {
    tokens = loginBulk();
    if (tokens.length === 0) throw new Error('Setup failed: bulk login returned 0 tokens');
  } else {
    const token = login();
    if (!token) throw new Error('Setup failed: cannot login');
    tokens = [token];
  }

  console.log(`Setup complete: ${tokens.length} tokens, FLASH_50=${FLASH_50_ID}, FLASH_100=${FLASH_100_ID}`);
  return { tokens, flash50Id: FLASH_50_ID, flash100Id: FLASH_100_ID };
}

export default function (data) {
  const token = USE_BULK ? getTokenForVU(data.tokens) : data.tokens[0];
  const params = authHeaders(token);

  // Select coupon based on scenario
  const couponId = __ENV.COUPON_TARGET === 'flash_100' ? data.flash100Id : data.flash50Id;

  const res = issueCoupon(params, couponId);

  check(res, {
    'coupon status valid': (r) => [200, 400, 409, 429, 503].includes(r.status),
  });

  if (res.status === 200) {
    couponIssued.add(1);
  } else if (res.status === 400 || res.status === 409) {
    // Distinguish sold out vs duplicate
    try {
      const body = res.json();
      const msg = (body.message || body.error || '').toLowerCase();
      if (msg.includes('sold') || msg.includes('exhausted') || msg.includes('quantity')) {
        couponSoldOut.add(1);
      } else if (msg.includes('duplicate') || msg.includes('already') || msg.includes('issued')) {
        couponDuplicate.add(1);
      } else {
        couponSoldOut.add(1);
      }
    } catch (_) {
      couponSoldOut.add(1);
    }
  } else if (res.status === 429) {
    couponRateLimited.add(1);
  }
}
