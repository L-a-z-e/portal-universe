/**
 * S3: 타임딜 러시 (Time Deal Rush)
 *
 * 목적: 타임딜 한정 수량 판매의 동시성 안전성 검증
 * - 200명이 점진적으로 유입되어 타임딜 상품(수량 50) 구매 시도
 * - 50개까지만 성공, 나머지는 품절 처리
 * - Over-selling 방지 (soldQuantity <= dealQuantity)
 *
 * Portfolio Theme 1: 분산 환경에서의 재고 정합성
 */
import { check, sleep } from 'k6';
import { Counter, Trend, Rate } from 'k6/metrics';
import { loginBulk, getTokenForVU, authHeaders } from '../lib/auth.js';
import { getActiveTimeDeals, purchaseTimeDeal, waitForCQRSSync } from '../lib/shopping-helpers.js';
import { checkValidStatus } from '../lib/checks.js';

// Custom metrics
const purchaseSuccess = new Counter('timedeal_purchase_success');
const purchaseSoldOut = new Counter('timedeal_sold_out');
const purchaseLimitExceeded = new Counter('timedeal_limit_exceeded');
const purchaseFailed = new Counter('timedeal_purchase_failed');
const purchaseDuration = new Trend('timedeal_purchase_duration_ms');
const purchaseSuccessRate = new Rate('timedeal_success_rate');

export const options = {
  setupTimeout: '180s',
  scenarios: {
    timedeal_rush: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 100 },  // rapid entry
        { duration: '2m', target: 200 },    // peak rush
        { duration: '3m', target: 200 },    // sustained (most stock depleted here)
        { duration: '1m', target: 100 },    // cool down
        { duration: '30s', target: 0 },     // drain
      ],
      gracefulRampDown: '30s',
    },
  },
  thresholds: {
    'http_req_duration{name:purchase_time_deal}': ['p(95)<5000'],
  },
};

export function setup() {
  const tokens = loginBulk(200);
  if (tokens.length === 0) throw new Error('Setup failed: bulk login returned 0 tokens');

  const adminParams = authHeaders(tokens[0]);
  waitForCQRSSync(adminParams, 50, 20);

  // Fetch active time deals
  const res = getActiveTimeDeals(adminParams);
  let timeDealProducts = [];
  let targetDeal = null;

  if (res.status === 200) {
    try {
      const deals = res.json('data.items') || res.json('data.content') || res.json('data') || [];
      for (const deal of deals) {
        const products = deal.products || [];
        for (const p of products) {
          const tdp = {
            timeDealProductId: p.id || p.timeDealProductId,
            dealQuantity: p.dealQuantity || 0,
            soldQuantity: p.soldQuantity || 0,
            maxPerUser: p.maxPerUser || 1,
            timeDealId: deal.id || deal.timeDealId,
          };
          timeDealProducts.push(tdp);
          if (!targetDeal && (tdp.dealQuantity - tdp.soldQuantity) > 0) {
            targetDeal = tdp;
          }
        }
      }
    } catch (e) {
      console.warn(`Failed to parse time deals: ${e.message}`);
    }
  }

  if (!targetDeal && timeDealProducts.length > 0) {
    targetDeal = timeDealProducts[0];
  }

  if (targetDeal) {
    console.log(`Setup: ${tokens.length} tokens, target timedeal product=${targetDeal.timeDealProductId}, remaining=${targetDeal.dealQuantity - targetDeal.soldQuantity}/${targetDeal.dealQuantity}`);
  } else {
    console.warn('No active time deal products found — test may produce no results');
  }

  return { tokens, targetDeal, timeDealProducts };
}

export default function (data) {
  const token = getTokenForVU(data.tokens);
  const params = authHeaders(token);

  if (!data.targetDeal) {
    sleep(1);
    return;
  }

  const startTime = Date.now();
  const res = purchaseTimeDeal(params, data.targetDeal.timeDealProductId, 1);
  purchaseDuration.add(Date.now() - startTime);

  check(res, {
    'timedeal status valid': (r) => [200, 400, 409, 429, 503].includes(r.status),
  });

  if (res.status === 200) {
    purchaseSuccess.add(1);
    purchaseSuccessRate.add(true);
  } else if (res.status === 400 || res.status === 409) {
    purchaseSuccessRate.add(false);
    try {
      const body = res.json();
      const msg = (body.message || body.error || '').toLowerCase();
      if (msg.includes('sold') || msg.includes('quantity') || msg.includes('stock') || msg.includes('품절')) {
        purchaseSoldOut.add(1);
      } else if (msg.includes('limit') || msg.includes('max') || msg.includes('exceed') || msg.includes('초과')) {
        purchaseLimitExceeded.add(1);
      } else {
        purchaseSoldOut.add(1);
      }
    } catch (_) {
      purchaseSoldOut.add(1);
    }
  } else {
    purchaseFailed.add(1);
    purchaseSuccessRate.add(false);
  }

  sleep(0.5 + Math.random() * 1.5); // stagger requests
}

export function teardown(data) {
  if (!data || !data.tokens || data.tokens.length === 0 || !data.targetDeal) return;

  const params = authHeaders(data.tokens[0]);
  const tdId = data.targetDeal.timeDealId;

  console.log('\n========== TIMEDEAL INTEGRITY CHECK ==========');

  const res = getActiveTimeDeals(params);
  if (res.status === 200) {
    try {
      const deals = res.json('data.items') || res.json('data.content') || res.json('data') || [];
      let oversellDetected = false;

      for (const deal of deals) {
        const products = deal.products || [];
        for (const p of products) {
          const sold = p.soldQuantity || 0;
          const total = p.dealQuantity || 0;
          const status = sold > total ? 'OVER-SOLD!' : 'OK';
          if (sold > total) oversellDetected = true;
          console.log(`  TimeDeal Product ${p.id || p.timeDealProductId}: sold=${sold}/${total} [${status}]`);
        }
      }

      console.log(`\n  Result: ${oversellDetected ? 'OVER-SELLING DETECTED' : 'No over-selling'}`);

      check(null, {
        'no timedeal over-selling': () => !oversellDetected,
      });
    } catch (e) {
      console.log(`  Error: ${e.message}`);
    }
  } else {
    console.log(`  Failed to get time deals: HTTP ${res.status}`);
  }

  console.log('================================================\n');
}
