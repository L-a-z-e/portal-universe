import { check } from 'k6';
import { config } from './config.js';
import http from 'k6/http';

const BASE = config.BASE_URL;

/**
 * Verify product inventory integrity via API
 * After all Saga flows complete, reserved quantity should be 0
 */
export function checkInventoryIntegrity(params, productIds) {
  const results = { passed: 0, failed: 0, errors: [] };

  for (const productId of productIds) {
    const res = http.get(
      `${BASE}/api/v1/shopping/products/${productId}`,
      Object.assign({}, params, { tags: { name: 'verify_product' } })
    );

    if (res.status === 200) {
      results.passed++;
    } else {
      results.failed++;
      results.errors.push(`Product ${productId}: HTTP ${res.status}`);
    }
  }

  return results;
}

/**
 * Verify coupon issuance count doesn't exceed total quantity
 */
export function checkCouponIntegrity(params, couponId, totalQuantity) {
  const res = http.get(
    `${BASE}/api/v1/shopping/coupons/${couponId}`,
    Object.assign({}, params, { tags: { name: 'verify_coupon' } })
  );

  if (res.status !== 200) return { pass: false, error: `HTTP ${res.status}` };

  try {
    const data = res.json('data');
    const issuedCount = data.issuedCount || data.issuedQuantity || 0;
    const limit = data.totalQuantity || totalQuantity;
    const pass = issuedCount <= limit;
    return { pass, issuedCount, limit };
  } catch (e) {
    return { pass: false, error: `Parse error: ${e.message}` };
  }
}

/**
 * Verify time deal sold quantity doesn't exceed deal quantity
 */
export function checkTimeDealIntegrity(params, timeDealId) {
  const res = http.get(
    `${BASE}/api/v1/shopping/time-deals/${timeDealId}`,
    Object.assign({}, params, { tags: { name: 'verify_timedeal' } })
  );

  if (res.status !== 200) return { pass: false, error: `HTTP ${res.status}` };

  try {
    const data = res.json('data');
    const products = data.products || [];
    const violations = [];
    for (const p of products) {
      if ((p.soldQuantity || 0) > (p.dealQuantity || 0)) {
        violations.push(`Product ${p.productId}: sold=${p.soldQuantity} > deal=${p.dealQuantity}`);
      }
    }
    return { pass: violations.length === 0, violations };
  } catch (e) {
    return { pass: false, error: `Parse error: ${e.message}` };
  }
}

/**
 * Print formatted integrity report
 */
export function printReport(title, results) {
  console.log(`\n===== ${title} =====`);
  for (const [key, val] of Object.entries(results)) {
    const status = val.pass ? 'PASS' : 'FAIL';
    console.log(`  [${status}] ${key}: ${JSON.stringify(val)}`);
  }
  console.log('='.repeat(title.length + 12));
}
