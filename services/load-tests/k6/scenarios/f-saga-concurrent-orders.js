import { check, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';
import { login, loginBulk, getTokenForVU, authHeaders } from '../lib/auth.js';
import { config } from '../lib/config.js';
import {
  addToCart, checkout, createOrder, confirmPayment,
  getProducts, waitForCQRSSync,
} from '../lib/shopping-helpers.js';
import { checkValidStatus } from '../lib/checks.js';

const USE_BULK = __ENV.USE_BULK === 'true';

// Custom metrics
const orderSuccess = new Counter('order_success');
const orderContention = new Counter('order_contention');
const orderFailed = new Counter('order_failed');
const paymentSuccess = new Counter('payment_success');
const paymentFailed = new Counter('payment_failed');
const orderFlowDuration = new Trend('order_flow_duration');

export const options = {
  setupTimeout: '180s',
  scenarios: {
    distributed_orders: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m', target: 100 },
        { duration: '2m', target: 500 },
        { duration: '2m', target: 500 },
        { duration: '1m', target: 0 },
      ],
      gracefulRampDown: '30s',
    },
    hotspot_orders: {
      executor: 'shared-iterations',
      vus: 200,
      iterations: 200,
      maxDuration: '3m',
      startTime: '7m',
    },
  },
  thresholds: {
    'http_req_duration{name:create_order}': ['p(95)<3000'],
    'http_req_duration{name:confirm_payment}': ['p(95)<3000'],
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

  // Wait for CQRS sync
  const adminParams = authHeaders(tokens[0]);
  const synced = waitForCQRSSync(adminParams, 100, 30);
  if (!synced) console.warn('CQRS sync may be incomplete — proceeding anyway');

  // Fetch available product IDs
  const res = getProducts(adminParams, 1, 100);
  let productIds = [];
  if (res.status === 200) {
    try {
      const items = res.json('data.items') || res.json('data.content') || [];
      productIds = items.map((p) => p.id || p.productId).filter(Boolean);
    } catch (_) { /* fallback */ }
  }
  if (productIds.length === 0) {
    // Fallback: use sequential IDs 1-105
    for (let i = 1; i <= 105; i++) productIds.push(i);
  }

  // Pick a hotspot product for the contention scenario
  const hotspotProductId = productIds[0];

  console.log(`Setup complete: ${tokens.length} tokens, ${productIds.length} products, hotspot=${hotspotProductId}`);
  return { tokens, productIds, hotspotProductId };
}

export default function (data) {
  const token = USE_BULK ? getTokenForVU(data.tokens) : data.tokens[0];
  const params = authHeaders(token);
  const startTime = Date.now();

  // Choose product based on scenario
  let productId;
  if (__ENV.SCENARIO === 'hotspot_orders' || (data.hotspotProductId && __VU > 300)) {
    productId = data.hotspotProductId;
  } else {
    productId = data.productIds[Math.floor(Math.random() * data.productIds.length)];
  }

  // Step 1: Add to cart
  const cartRes = addToCart(params, productId, 1);
  if (cartRes.status !== 200) {
    checkValidStatus(cartRes, 'add_to_cart', [200, 400, 409, 429, 503]);
    orderFailed.add(1);
    return;
  }
  sleep(0.1);

  // Step 2: Checkout
  const checkoutRes = checkout(params);
  if (checkoutRes.status !== 200) {
    checkValidStatus(checkoutRes, 'checkout', [200, 400, 409, 429, 503]);
    orderFailed.add(1);
    return;
  }
  sleep(0.1);

  // Step 3: Create order (Saga starts: inventory RESERVE)
  const orderRes = createOrder(params);
  if (orderRes.status === 200 || orderRes.status === 201) {
    orderSuccess.add(1);
  } else if (orderRes.status === 409 || orderRes.status === 400) {
    orderContention.add(1);
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
    console.warn('No intentId in order response');
    orderFlowDuration.add(Date.now() - startTime);
    return;
  }

  sleep(0.2);
  const paymentRes = confirmPayment(params, intentId);
  if (paymentRes.status === 200) {
    paymentSuccess.add(1);
  } else {
    paymentFailed.add(1);
    checkValidStatus(paymentRes, 'confirm_payment', [200, 400, 409, 422, 429, 503]);
  }

  orderFlowDuration.add(Date.now() - startTime);
  sleep(0.5);
}
