import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';
import { login, loginBulk, getTokenForVU, authHeaders } from '../lib/auth.js';
import { config } from '../lib/config.js';
import { getActiveTimeDeals, purchaseTimeDeal, waitForCQRSSync } from '../lib/shopping-helpers.js';

const USE_BULK = __ENV.USE_BULK === 'true';

// Custom metrics
const purchaseSuccess = new Counter('purchase_success');
const soldOut = new Counter('sold_out');
const limitExceeded = new Counter('limit_exceeded');
const purchaseFailed = new Counter('purchase_failed');

export const options = {
  scenarios: {
    timedeal_rush: {
      executor: 'shared-iterations',
      vus: 500,
      iterations: 500,
      maxDuration: '1m',
    },
  },
  thresholds: {
    'http_req_duration{name:purchase_time_deal}': ['p(95)<3000'],
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

  // Wait for CQRS sync before fetching time deals
  const adminParams = authHeaders(tokens[0]);
  waitForCQRSSync(adminParams, 100, 20);

  // Fetch active time deals and their product IDs
  const res = getActiveTimeDeals(adminParams);
  let timeDealProducts = [];
  if (res.status === 200) {
    try {
      const deals = res.json('data.items') || res.json('data.content') || res.json('data') || [];
      for (const deal of deals) {
        const products = deal.products || [];
        for (const p of products) {
          timeDealProducts.push({
            timeDealProductId: p.id || p.timeDealProductId,
            dealQuantity: p.dealQuantity,
            maxPerUser: p.maxPerUser,
          });
        }
      }
    } catch (e) {
      console.warn(`Failed to parse time deals: ${e.message}`);
    }
  }

  if (timeDealProducts.length === 0) {
    console.warn('No active time deal products found — test may not produce meaningful results');
  }

  console.log(`Setup complete: ${tokens.length} tokens, ${timeDealProducts.length} time deal products`);
  return { tokens, timeDealProducts };
}

export default function (data) {
  const token = USE_BULK ? getTokenForVU(data.tokens) : data.tokens[0];
  const params = authHeaders(token);

  if (data.timeDealProducts.length === 0) {
    console.warn('No time deal products available, skipping');
    return;
  }

  // Pick the first time deal product (highest contention on single product)
  const target = data.timeDealProducts[0];
  const res = purchaseTimeDeal(params, target.timeDealProductId, 1);

  check(res, {
    'timedeal status valid': (r) => [200, 400, 409, 429, 503].includes(r.status),
  });

  if (res.status === 200) {
    purchaseSuccess.add(1);
  } else if (res.status === 400 || res.status === 409) {
    try {
      const body = res.json();
      const msg = (body.message || body.error || '').toLowerCase();
      if (msg.includes('sold') || msg.includes('quantity') || msg.includes('stock')) {
        soldOut.add(1);
      } else if (msg.includes('limit') || msg.includes('max') || msg.includes('exceed')) {
        limitExceeded.add(1);
      } else {
        soldOut.add(1);
      }
    } catch (_) {
      soldOut.add(1);
    }
  } else {
    purchaseFailed.add(1);
  }
}
