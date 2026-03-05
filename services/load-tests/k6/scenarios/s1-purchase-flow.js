/**
 * S1: 일반 구매 플로우 (Normal Purchase Flow)
 *
 * 목적: Saga Orchestrator의 정상 동작 검증 + 자연스러운 재고 소진 패턴
 * - 200명의 사용자가 점진적으로 유입되어 구매
 * - 인기 상품(재고 100)은 자연스럽게 품절되고, 일반 상품(재고 300)은 여유
 * - 재고 소진 시 Saga 보상 트랜잭션 발동 확인
 * - Over-selling 방지 검증 (잔여 재고 >= 0)
 *
 * Portfolio Theme 1: Saga Orchestrator + Outbox 분산 트랜잭션
 */
import { check, sleep } from 'k6';
import { Counter, Trend, Rate } from 'k6/metrics';
import { loginBulk, getTokenForVU, authHeaders } from '../lib/auth.js';
import {
  addToCart, checkout, createOrder, confirmPayment,
  getProducts, getInventory, waitForCQRSSync,
} from '../lib/shopping-helpers.js';
import { checkValidStatus } from '../lib/checks.js';

// Custom metrics
const orderSuccess = new Counter('saga_order_success');
const orderStockOut = new Counter('saga_order_stock_out');
const orderFailed = new Counter('saga_order_failed');
const paymentSuccess = new Counter('saga_payment_success');
const paymentFailed = new Counter('saga_payment_failed');
const sagaFlowDuration = new Trend('saga_flow_duration_ms');
const sagaSuccessRate = new Rate('saga_success_rate');

export const options = {
  setupTimeout: '180s',
  scenarios: {
    purchase_flow: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m', target: 50 },   // warm-up
        { duration: '2m', target: 100 },   // ramp to peak
        { duration: '3m', target: 100 },   // sustained peak
        { duration: '1m', target: 50 },    // cool down
        { duration: '1m', target: 0 },     // ramp down
      ],
      gracefulRampDown: '30s',
    },
  },
  thresholds: {
    'http_req_duration{name:create_order}': ['p(95)<5000'],
    'http_req_duration{name:confirm_payment}': ['p(95)<5000'],
    saga_success_rate: ['rate>0.3'],
  },
};

export function setup() {
  const tokens = loginBulk(200);
  if (tokens.length === 0) throw new Error('Setup failed: bulk login returned 0 tokens');

  const adminParams = authHeaders(tokens[0]);
  const synced = waitForCQRSSync(adminParams, 50, 30);
  if (!synced) console.warn('CQRS sync may be incomplete');

  // Fetch product list
  const res = getProducts(adminParams, 1, 100);
  let productIds = [];
  if (res.status === 200) {
    try {
      const items = res.json('data.items') || res.json('data.content') || [];
      productIds = items.map((p) => p.id || p.productId).filter(Boolean);
    } catch (_) { /* fallback */ }
  }
  if (productIds.length === 0) {
    for (let i = 1; i <= 50; i++) productIds.push(i);
  }

  // Separate popular products (first 5) and regular products (rest)
  const popularProducts = productIds.slice(0, 5);
  const regularProducts = productIds.slice(5);

  // Record initial inventory for popular products
  const initialInventory = {};
  for (const pid of popularProducts) {
    const invRes = getInventory(adminParams, pid);
    if (invRes.status === 200) {
      try {
        const inv = invRes.json('data');
        initialInventory[pid] = inv.availableQuantity || inv.totalQuantity || 0;
      } catch (_) { /* ignore */ }
    }
  }

  console.log(`Setup: ${tokens.length} tokens, ${productIds.length} products (${popularProducts.length} popular), initial inventory: ${JSON.stringify(initialInventory)}`);
  return { tokens, popularProducts, regularProducts, initialInventory };
}

export default function (data) {
  const token = getTokenForVU(data.tokens);
  const params = authHeaders(token);
  const startTime = Date.now();

  // 70% chance to pick popular product (creates natural contention)
  // 30% chance to pick regular product
  let productId;
  if (Math.random() < 0.7 && data.popularProducts.length > 0) {
    productId = data.popularProducts[Math.floor(Math.random() * data.popularProducts.length)];
  } else if (data.regularProducts.length > 0) {
    productId = data.regularProducts[Math.floor(Math.random() * data.regularProducts.length)];
  } else {
    productId = data.popularProducts[0];
  }

  // Step 1: Add to cart
  const cartRes = addToCart(params, productId, 1);
  if (cartRes.status !== 200) {
    checkValidStatus(cartRes, 'add_to_cart', [200, 400, 409, 429, 503]);
    orderFailed.add(1);
    sagaSuccessRate.add(false);
    sagaFlowDuration.add(Date.now() - startTime);
    sleep(1);
    return;
  }
  sleep(0.3 + Math.random() * 0.5); // realistic think time

  // Step 2: Checkout
  const checkoutRes = checkout(params);
  if (checkoutRes.status !== 200) {
    checkValidStatus(checkoutRes, 'checkout', [200, 400, 409, 429, 503]);
    orderFailed.add(1);
    sagaSuccessRate.add(false);
    sagaFlowDuration.add(Date.now() - startTime);
    sleep(1);
    return;
  }
  sleep(0.2);

  // Step 3: Create order (Saga begins: RESERVE_INVENTORY)
  const orderRes = createOrder(params);
  if (orderRes.status === 200 || orderRes.status === 201) {
    orderSuccess.add(1);
  } else if (orderRes.status === 409 || orderRes.status === 400) {
    // Stock exhausted → Saga compensation triggered
    orderStockOut.add(1);
    sagaSuccessRate.add(false);
    sagaFlowDuration.add(Date.now() - startTime);
    sleep(1);
    return;
  } else {
    checkValidStatus(orderRes, 'create_order', [200, 201, 400, 409, 422, 429, 503]);
    orderFailed.add(1);
    sagaSuccessRate.add(false);
    sagaFlowDuration.add(Date.now() - startTime);
    sleep(1);
    return;
  }

  // Step 4: Confirm payment (Saga: PROCESS_PAYMENT → DEDUCT_INVENTORY → ...)
  let intentId;
  try {
    const orderData = orderRes.json('data');
    intentId = orderData.intentId || orderData.paymentIntentId;
  } catch (_) {
    orderFailed.add(1);
    sagaSuccessRate.add(false);
    sagaFlowDuration.add(Date.now() - startTime);
    return;
  }

  if (!intentId) {
    console.warn(`VU${__VU}: No intentId in order response`);
    sagaSuccessRate.add(false);
    sagaFlowDuration.add(Date.now() - startTime);
    return;
  }

  sleep(0.5 + Math.random() * 1.0); // payment think time
  const paymentRes = confirmPayment(params, intentId);
  if (paymentRes.status === 200) {
    paymentSuccess.add(1);
    sagaSuccessRate.add(true);
  } else {
    paymentFailed.add(1);
    sagaSuccessRate.add(false);
    checkValidStatus(paymentRes, 'confirm_payment', [200, 400, 409, 422, 429, 503]);
  }

  sagaFlowDuration.add(Date.now() - startTime);
  sleep(1 + Math.random() * 2); // inter-purchase delay
}

export function teardown(data) {
  if (!data || !data.tokens || data.tokens.length === 0) return;

  const params = authHeaders(data.tokens[0]);
  console.log('\n========== OVER-SELLING CHECK ==========');

  let oversellDetected = false;
  for (const pid of data.popularProducts) {
    const invRes = getInventory(params, pid);
    if (invRes.status === 200) {
      try {
        const inv = invRes.json('data');
        const available = inv.availableQuantity;
        const initial = data.initialInventory[pid] || 'unknown';
        const status = available < 0 ? 'OVER-SOLD!' : 'OK';
        if (available < 0) oversellDetected = true;
        console.log(`  Product ${pid}: initial=${initial}, available=${available}, reserved=${inv.reservedQuantity} [${status}]`);
      } catch (_) {
        console.log(`  Product ${pid}: failed to read inventory`);
      }
    }
  }

  console.log(`\n  Result: ${oversellDetected ? 'OVER-SELLING DETECTED' : 'No over-selling'}`);
  console.log('=========================================\n');

  check(null, {
    'no over-selling detected': () => !oversellDetected,
  });
}
