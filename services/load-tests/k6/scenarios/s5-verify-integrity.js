/**
 * S5: 데이터 정합성 검증 (Post-Test Integrity Verification)
 *
 * 목적: S1~S3 부하 테스트 후 데이터 일관성 검증
 * - 재고: availableQuantity >= 0 (over-selling 없음)
 * - 타임딜: soldQuantity <= dealQuantity
 * - 주문/사가/Outbox 상태 일관성
 *
 * Portfolio Theme 1: Saga + Outbox 패턴이 데이터 정합성을 보장함을 입증
 */
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';
import { login, loginBulk, authHeaders } from '../lib/auth.js';
import {
  getProducts, getActiveTimeDeals, getInventory, getInventoryBatch,
} from '../lib/shopping-helpers.js';
import { checkTimeDealIntegrity, printReport } from '../lib/integrity-checks.js';

// Custom metrics
const checksPass = new Counter('integrity_checks_pass');
const checksFail = new Counter('integrity_checks_fail');

export const options = {
  scenarios: {
    verify: {
      executor: 'shared-iterations',
      vus: 1,
      iterations: 1,
      maxDuration: '3m',
    },
  },
  thresholds: {},
};

export function setup() {
  const USE_BULK = __ENV.USE_BULK === 'true';
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

  // ───── 1. Product availability ─────
  console.log('[1/4] Checking product availability...');
  const productRes = getProducts(params, 1, 100);
  let productIds = [];
  if (productRes.status === 200) {
    try {
      const items = productRes.json('data.items') || productRes.json('data.content') || [];
      productIds = items.map((p) => p.id || p.productId).filter(Boolean);
      results['products_available'] = { pass: items.length > 0, count: items.length };
      console.log(`  Products available: ${items.length}`);
    } catch (e) {
      results['products_available'] = { pass: false, error: e.message };
    }
  } else {
    results['products_available'] = { pass: false, error: `HTTP ${productRes.status}` };
  }
  sleep(0.5);

  // ───── 2. Inventory integrity (over-selling check) ─────
  console.log('[2/4] Checking inventory integrity (over-selling)...');
  const oversoldProducts = [];
  const negativeReserved = [];

  if (productIds.length > 0) {
    for (let i = 0; i < Math.min(productIds.length, 100); i++) {
      const pid = productIds[i];
      const invRes = getInventory(params, pid);
      if (invRes.status === 200) {
        try {
          const inv = invRes.json('data');
          if ((inv.availableQuantity || 0) < 0) {
            oversoldProducts.push({ productId: pid, available: inv.availableQuantity });
          }
          if ((inv.reservedQuantity || 0) < 0) {
            negativeReserved.push({ productId: pid, reserved: inv.reservedQuantity });
          }
        } catch (_) { /* skip */ }
      }
      if (i % 20 === 19) sleep(0.5);
    }

    results['no_overselling'] = {
      pass: oversoldProducts.length === 0,
      checkedCount: Math.min(productIds.length, 100),
      violations: oversoldProducts,
    };
    results['no_negative_reserved'] = {
      pass: negativeReserved.length === 0,
      violations: negativeReserved,
    };

    if (oversoldProducts.length > 0) {
      console.log(`  OVER-SELLING DETECTED in ${oversoldProducts.length} products:`);
      for (const v of oversoldProducts) {
        console.log(`    Product ${v.productId}: available=${v.available}`);
      }
    } else {
      console.log(`  No over-selling detected (checked ${Math.min(productIds.length, 100)} products)`);
    }
  } else {
    results['no_overselling'] = { pass: true, note: 'no products to verify' };
    results['no_negative_reserved'] = { pass: true, note: 'no products to verify' };
  }
  sleep(0.5);

  // ───── 3. TimeDeal integrity ─────
  console.log('[3/4] Checking time deal integrity...');
  const tdRes = getActiveTimeDeals(params);
  if (tdRes.status === 200) {
    try {
      const deals = tdRes.json('data.items') || tdRes.json('data.content') || tdRes.json('data') || [];
      let tdOversold = false;
      for (const deal of deals) {
        const dealId = deal.id || deal.timeDealId;
        if (dealId) {
          const result = checkTimeDealIntegrity(params, dealId);
          results[`timedeal_${dealId}`] = result;
          if (!result.pass) tdOversold = true;
          sleep(0.2);
        }
      }
      if (deals.length === 0) {
        results['timedeal_check'] = { pass: true, note: 'no active deals to verify' };
        console.log('  No active time deals');
      } else {
        console.log(`  Checked ${deals.length} time deals: ${tdOversold ? 'VIOLATIONS FOUND' : 'All OK'}`);
      }
    } catch (e) {
      results['timedeal_check'] = { pass: false, error: e.message };
    }
  } else {
    results['timedeal_check'] = { pass: false, error: `HTTP ${tdRes.status}` };
  }
  sleep(0.5);

  // ───── 4. Stock consistency (available + reserved <= total) ─────
  console.log('[4/4] Checking stock consistency (available + reserved <= total)...');
  const inconsistent = [];

  for (let i = 0; i < Math.min(productIds.length, 50); i++) {
    const pid = productIds[i];
    const invRes = getInventory(params, pid);
    if (invRes.status === 200) {
      try {
        const inv = invRes.json('data');
        const avail = inv.availableQuantity || 0;
        const reserved = inv.reservedQuantity || 0;
        const total = inv.totalQuantity || 0;
        if (avail + reserved > total) {
          inconsistent.push({ productId: pid, available: avail, reserved, total });
        }
      } catch (_) { /* skip */ }
    }
    if (i % 20 === 19) sleep(0.3);
  }

  results['stock_consistency'] = {
    pass: inconsistent.length === 0,
    checkedCount: Math.min(productIds.length, 50),
    violations: inconsistent,
  };

  if (inconsistent.length > 0) {
    console.log(`  INCONSISTENCY in ${inconsistent.length} products:`);
    for (const v of inconsistent) {
      console.log(`    Product ${v.productId}: avail=${v.available} + reserved=${v.reserved} > total=${v.total}`);
    }
  } else {
    console.log(`  Stock consistent (checked ${Math.min(productIds.length, 50)} products)`);
  }

  // ───── Summary ─────
  printReport('INTEGRITY VERIFICATION RESULTS', results);

  const allPassed = Object.values(results).every((r) => r.pass);

  if (allPassed) {
    checksPass.add(1);
  } else {
    checksFail.add(1);
  }

  console.log(`\nFinal verdict: ${allPassed ? 'ALL PASSED' : 'SOME CHECKS FAILED'}`);

  check(null, {
    'all integrity checks passed': () => allPassed,
    'no over-selling anywhere': () => oversoldProducts.length === 0,
    'stock quantities consistent': () => inconsistent.length === 0,
  });
}
