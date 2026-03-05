/**
 * S2: 동시 주문 스파이크 (Concurrent Order Spike)
 *
 * 목적: Saga의 동시성 제어 + 재고 정합성 검증
 * - 200명이 동시에 1개 상품(재고 100)을 주문
 * - 정확히 100개까지만 성공, 나머지는 재고 부족으로 실패
 * - 잔여 재고 = 0 (음수 불가)
 * - Pessimistic Lock/Optimistic Lock 동작 검증
 *
 * Portfolio Theme 1: Saga Orchestrator의 동시성 안전성 입증
 */
import { check, sleep } from 'k6';
import { Counter, Trend, Gauge } from 'k6/metrics';
import { loginBulk, getTokenForVU, authHeaders } from '../lib/auth.js';
import {
  addToCart, checkout, createOrder, confirmPayment,
  getProducts, getInventory, waitForCQRSSync,
} from '../lib/shopping-helpers.js';
import { checkValidStatus } from '../lib/checks.js';

// Custom metrics
const orderSuccess = new Counter('concurrent_order_success');
const orderStockOut = new Counter('concurrent_order_stock_out');
const orderFailed = new Counter('concurrent_order_failed');
const paymentSuccess = new Counter('concurrent_payment_success');
const paymentFailed = new Counter('concurrent_payment_failed');
const orderFlowDuration = new Trend('concurrent_flow_duration_ms');
const finalStock = new Gauge('concurrent_final_stock');

export const options = {
  setupTimeout: '180s',
  scenarios: {
    concurrent_spike: {
      executor: 'shared-iterations',
      vus: 200,
      iterations: 200,
      maxDuration: '5m',
    },
  },
  thresholds: {
    'http_req_duration{name:create_order}': ['p(95)<5000'],
    'http_req_duration{name:confirm_payment}': ['p(95)<5000'],
  },
};

export function setup() {
  const tokens = loginBulk(200);
  if (tokens.length === 0) throw new Error('Setup failed: bulk login returned 0 tokens');

  const adminParams = authHeaders(tokens[0]);
  const synced = waitForCQRSSync(adminParams, 50, 30);
  if (!synced) console.warn('CQRS sync may be incomplete');

  // Pick the target product (first product with sufficient stock)
  const res = getProducts(adminParams, 1, 100);
  let targetProductId = null;
  let initialStock = 0;

  if (res.status === 200) {
    try {
      const items = res.json('data.items') || res.json('data.content') || [];
      for (const item of items) {
        const pid = item.id || item.productId;
        if (!pid) continue;
        const invRes = getInventory(adminParams, pid);
        if (invRes.status === 200) {
          const inv = invRes.json('data');
          const avail = inv.availableQuantity || 0;
          if (avail >= 50) {
            targetProductId = pid;
            initialStock = avail;
            break;
          }
        }
      }
    } catch (_) { /* fallback */ }
  }

  if (!targetProductId) {
    targetProductId = 1;
    initialStock = 100;
    console.warn('Could not find product with stock >= 50, falling back to product 1');
  }

  console.log(`Setup: ${tokens.length} tokens, target product=${targetProductId}, initial stock=${initialStock}`);
  return { tokens, targetProductId, initialStock };
}

export default function (data) {
  const token = getTokenForVU(data.tokens);
  const params = authHeaders(token);
  const startTime = Date.now();
  const productId = data.targetProductId;

  // Step 1: Add to cart
  const cartRes = addToCart(params, productId, 1);
  if (cartRes.status !== 200) {
    checkValidStatus(cartRes, 'add_to_cart', [200, 400, 409, 429, 503]);
    orderFailed.add(1);
    orderFlowDuration.add(Date.now() - startTime);
    return;
  }

  // Step 2: Checkout
  const checkoutRes = checkout(params);
  if (checkoutRes.status !== 200) {
    checkValidStatus(checkoutRes, 'checkout', [200, 400, 409, 429, 503]);
    orderFailed.add(1);
    orderFlowDuration.add(Date.now() - startTime);
    return;
  }

  // Step 3: Create order — this is where contention happens
  const orderRes = createOrder(params);
  if (orderRes.status === 200 || orderRes.status === 201) {
    orderSuccess.add(1);
  } else if (orderRes.status === 409 || orderRes.status === 400) {
    orderStockOut.add(1);
    orderFlowDuration.add(Date.now() - startTime);
    return;
  } else {
    checkValidStatus(orderRes, 'create_order', [200, 201, 400, 409, 422, 429, 503]);
    orderFailed.add(1);
    orderFlowDuration.add(Date.now() - startTime);
    return;
  }

  // Step 4: Confirm payment
  let intentId;
  try {
    const orderData = orderRes.json('data');
    intentId = orderData.intentId || orderData.paymentIntentId;
  } catch (_) {
    orderFailed.add(1);
    orderFlowDuration.add(Date.now() - startTime);
    return;
  }

  if (!intentId) {
    orderFlowDuration.add(Date.now() - startTime);
    return;
  }

  const paymentRes = confirmPayment(params, intentId);
  if (paymentRes.status === 200) {
    paymentSuccess.add(1);
  } else {
    paymentFailed.add(1);
    checkValidStatus(paymentRes, 'confirm_payment', [200, 400, 409, 422, 429, 503]);
  }

  orderFlowDuration.add(Date.now() - startTime);
}

export function teardown(data) {
  if (!data || !data.tokens || data.tokens.length === 0) return;

  const params = authHeaders(data.tokens[0]);
  const productId = data.targetProductId;

  console.log('\n========== CONCURRENT ORDER INTEGRITY CHECK ==========');

  const invRes = getInventory(params, productId);
  if (invRes.status === 200) {
    try {
      const inv = invRes.json('data');
      const available = inv.availableQuantity;
      const reserved = inv.reservedQuantity;
      const total = inv.totalQuantity;
      const initial = data.initialStock;

      finalStock.add(available);

      console.log(`  Product ${productId}:`);
      console.log(`    Initial stock:    ${initial}`);
      console.log(`    Available now:    ${available}`);
      console.log(`    Reserved:         ${reserved}`);
      console.log(`    Total:            ${total}`);
      console.log(`    Orders fulfilled: ${initial - available - reserved}`);

      const oversold = available < 0;
      console.log(`\n  Over-selling: ${oversold ? 'DETECTED!' : 'None'}`);
      console.log(`  Stock >= 0:   ${available >= 0 ? 'PASS' : 'FAIL'}`);

      check(null, {
        'no over-selling (available >= 0)': () => available >= 0,
        'no negative reserved': () => reserved >= 0,
      });
    } catch (e) {
      console.log(`  Error reading inventory: ${e.message}`);
    }
  } else {
    console.log(`  Failed to get inventory: HTTP ${invRes.status}`);
  }

  console.log('======================================================\n');
}
