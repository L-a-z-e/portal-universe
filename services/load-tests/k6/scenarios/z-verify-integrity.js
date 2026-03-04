import { check, sleep } from 'k6';
import { login, loginBulk, authHeaders } from '../lib/auth.js';
import { config } from '../lib/config.js';
import { getProducts, getActiveTimeDeals } from '../lib/shopping-helpers.js';
import { checkCouponIntegrity, checkTimeDealIntegrity, printReport } from '../lib/integrity-checks.js';
import http from 'k6/http';

const USE_BULK = __ENV.USE_BULK === 'true';
const BASE = config.BASE_URL;

// Flash coupon IDs (from seed data)
const FLASH_50_ID = __ENV.FLASH_50_ID || '26';
const FLASH_100_ID = __ENV.FLASH_100_ID || '27';

export const options = {
  scenarios: {
    verify: {
      executor: 'shared-iterations',
      vus: 1,
      iterations: 1,
      maxDuration: '2m',
    },
  },
  thresholds: {},
};

export function setup() {
  let token;
  if (USE_BULK) {
    const tokens = loginBulk(1);
    token = tokens[0];
  } else {
    token = login();
  }
  if (!token) throw new Error('Setup failed: cannot login');
  return { token };
}

export default function (data) {
  const params = authHeaders(data.token);
  const results = {};

  console.log('\n========================================');
  console.log('  POST-TEST INTEGRITY VERIFICATION');
  console.log('========================================\n');

  // 1. Product inventory check — all products should be accessible
  console.log('Checking product availability...');
  const productRes = getProducts(params, 1, 100);
  if (productRes.status === 200) {
    try {
      const items = productRes.json('data.items') || productRes.json('data.content') || [];
      results['products_available'] = { pass: items.length > 0, count: items.length };
      console.log(`  Products available: ${items.length}`);
    } catch (e) {
      results['products_available'] = { pass: false, error: e.message };
    }
  } else {
    results['products_available'] = { pass: false, error: `HTTP ${productRes.status}` };
  }

  sleep(0.5);

  // 2. Flash coupon integrity — issued count <= total quantity
  console.log('Checking coupon integrity...');
  results['flash_50_coupon'] = checkCouponIntegrity(params, FLASH_50_ID, 50);
  sleep(0.2);
  results['flash_100_coupon'] = checkCouponIntegrity(params, FLASH_100_ID, 100);

  sleep(0.5);

  // 3. TimeDeal integrity — sold quantity <= deal quantity
  console.log('Checking time deal integrity...');
  const tdRes = getActiveTimeDeals(params);
  if (tdRes.status === 200) {
    try {
      const deals = tdRes.json('data.items') || tdRes.json('data.content') || tdRes.json('data') || [];
      for (const deal of deals) {
        const dealId = deal.id || deal.timeDealId;
        if (dealId) {
          results[`timedeal_${dealId}`] = checkTimeDealIntegrity(params, dealId);
          sleep(0.2);
        }
      }
      if (deals.length === 0) {
        results['timedeal_check'] = { pass: true, note: 'no active deals to verify' };
      }
    } catch (e) {
      results['timedeal_check'] = { pass: false, error: e.message };
    }
  } else {
    results['timedeal_check'] = { pass: false, error: `HTTP ${tdRes.status}` };
  }

  // Print summary report
  printReport('INTEGRITY VERIFICATION RESULTS', results);

  // Final verdict
  const allPassed = Object.values(results).every((r) => r.pass);
  console.log(`\nFinal verdict: ${allPassed ? 'ALL PASSED' : 'SOME CHECKS FAILED'}`);

  check(null, {
    'all integrity checks passed': () => allPassed,
  });
}
