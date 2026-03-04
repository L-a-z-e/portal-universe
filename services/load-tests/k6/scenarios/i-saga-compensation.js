import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';
import { login, loginBulk, getTokenForVU, authHeaders } from '../lib/auth.js';
import { config } from '../lib/config.js';
import {
  addToCart, checkout, createOrder, cancelOrder,
  getOrder, getProducts, waitForCQRSSync,
} from '../lib/shopping-helpers.js';
import { checkValidStatus } from '../lib/checks.js';

const USE_BULK = __ENV.USE_BULK === 'true';

// Custom metrics
const cancelSuccess = new Counter('cancel_success');
const cancelFailed = new Counter('cancel_failed');
const orderCreated = new Counter('order_created');
const orderCreateFailed = new Counter('order_create_failed');

export const options = {
  scenarios: {
    order_then_cancel: {
      executor: 'per-vu-iterations',
      vus: 100,
      iterations: 1,
      maxDuration: '3m',
    },
  },
  thresholds: {
    'http_req_duration{name:create_order}': ['p(95)<3000'],
    'http_req_duration{name:cancel_order}': ['p(95)<3000'],
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
  if (!synced) console.warn('CQRS sync may be incomplete');

  // Fetch product IDs
  const res = getProducts(adminParams, 1, 50);
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

  console.log(`Setup complete: ${tokens.length} tokens, ${productIds.length} products`);
  return { tokens, productIds };
}

export default function (data) {
  const token = USE_BULK ? getTokenForVU(data.tokens) : data.tokens[0];
  const params = authHeaders(token);

  // Pick a random product
  const productId = data.productIds[Math.floor(Math.random() * data.productIds.length)];

  // Step 1: Add to cart
  const cartRes = addToCart(params, productId, 1);
  if (cartRes.status !== 200) {
    checkValidStatus(cartRes, 'add_to_cart', [200, 400, 409, 429, 503]);
    orderCreateFailed.add(1);
    return;
  }

  // Step 2: Checkout
  const checkoutRes = checkout(params);
  if (checkoutRes.status !== 200) {
    checkValidStatus(checkoutRes, 'checkout', [200, 400, 409, 429, 503]);
    orderCreateFailed.add(1);
    return;
  }

  // Step 3: Create order (Saga RESERVE completes)
  const orderRes = createOrder(params);
  if (orderRes.status !== 200 && orderRes.status !== 201) {
    checkValidStatus(orderRes, 'create_order', [200, 201, 400, 409, 429, 503]);
    orderCreateFailed.add(1);
    return;
  }

  orderCreated.add(1);

  let orderNumber;
  try {
    const orderData = orderRes.json('data');
    orderNumber = orderData.orderNumber;
  } catch (_) {
    orderCreateFailed.add(1);
    return;
  }

  if (!orderNumber) {
    console.warn('No orderNumber in response');
    return;
  }

  // Step 4: Do NOT confirm payment — instead cancel immediately
  // This triggers Saga compensation (RELEASE inventory)
  sleep(0.5);

  const cancelRes = cancelOrder(params, orderNumber, 'load test compensation verification');

  check(cancelRes, {
    'cancel status valid': (r) => [200, 400, 409, 422, 429, 503].includes(r.status),
  });

  if (cancelRes.status === 200) {
    cancelSuccess.add(1);

    // Verify order status is CANCELLED
    sleep(1);
    const verifyRes = getOrder(params, orderNumber);
    if (verifyRes.status === 200) {
      try {
        const status = verifyRes.json('data.status') || verifyRes.json('data.orderStatus');
        check(verifyRes, {
          'order cancelled after compensation': () =>
            status === 'CANCELLED' || status === 'CANCEL_REQUESTED',
        });
      } catch (_) { /* best effort */ }
    }
  } else {
    cancelFailed.add(1);
  }
}
