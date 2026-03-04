import http from 'k6/http';
import { sleep } from 'k6';
import { config } from './config.js';

const BASE = config.BASE_URL;

/**
 * Add item to cart
 */
export function addToCart(params, productId, quantity) {
  return http.post(
    `${BASE}/api/v1/shopping/cart/items`,
    JSON.stringify({ productId, quantity: quantity || 1 }),
    Object.assign({}, params, { tags: { name: 'add_to_cart' } })
  );
}

/**
 * Checkout current cart
 */
export function checkout(params) {
  return http.post(
    `${BASE}/api/v1/shopping/cart/checkout`,
    null,
    Object.assign({}, params, { tags: { name: 'checkout' } })
  );
}

/**
 * Create order from checked-out cart
 */
export function createOrder(params, shippingAddress, userCouponId) {
  const body = {
    shippingAddress: shippingAddress || {
      receiverName: '테스트',
      receiverPhone: '010-1234-5678',
      zipCode: '06234',
      address1: '서울시 강남구 테스트동 123',
      address2: '101호',
    },
  };
  if (userCouponId) body.userCouponId = userCouponId;

  return http.post(
    `${BASE}/api/v1/shopping/orders`,
    JSON.stringify(body),
    Object.assign({}, params, { tags: { name: 'create_order' } })
  );
}

/**
 * Confirm payment intent
 */
export function confirmPayment(params, intentId) {
  return http.post(
    `${BASE}/api/v1/payment/intents/${intentId}/confirm`,
    JSON.stringify({ paymentMethod: 'CARD', cardNumber: '4242424242424242', cardExpiry: '12/28', cardCvv: '123' }),
    Object.assign({}, params, { tags: { name: 'confirm_payment' } })
  );
}

/**
 * Cancel order
 */
export function cancelOrder(params, orderNumber, reason) {
  return http.post(
    `${BASE}/api/v1/shopping/orders/${orderNumber}/cancel`,
    JSON.stringify({ reason: reason || 'load test cancellation' }),
    Object.assign({}, params, { tags: { name: 'cancel_order' } })
  );
}

/**
 * Full order flow: addToCart → checkout → createOrder → confirmPayment
 * Returns { orderRes, paymentRes, intentId, orderNumber }
 */
export function fullOrderFlow(params, productId) {
  const cartRes = addToCart(params, productId, 1);
  if (cartRes.status !== 200) return { cartRes, success: false, stage: 'cart' };

  const checkoutRes = checkout(params);
  if (checkoutRes.status !== 200) return { checkoutRes, success: false, stage: 'checkout' };

  const orderRes = createOrder(params);
  if (orderRes.status !== 200 && orderRes.status !== 201) {
    return { orderRes, success: false, stage: 'order' };
  }

  let intentId, orderNumber;
  try {
    const data = orderRes.json('data');
    intentId = data.intentId || data.paymentIntentId;
    orderNumber = data.orderNumber;
  } catch (_) {
    return { orderRes, success: false, stage: 'parse_order' };
  }

  if (!intentId) return { orderRes, success: false, stage: 'no_intent' };

  const paymentRes = confirmPayment(params, intentId);
  return {
    orderRes,
    paymentRes,
    intentId,
    orderNumber,
    success: paymentRes.status === 200,
    stage: 'complete',
  };
}

/**
 * Get product list
 */
export function getProducts(params, page, size) {
  return http.get(
    `${BASE}/api/v1/shopping/products?page=${page || 1}&size=${size || 20}`,
    Object.assign({}, params, { tags: { name: 'get_products' } })
  );
}

/**
 * Get active time deals
 */
export function getActiveTimeDeals(params) {
  return http.get(
    `${BASE}/api/v1/shopping/time-deals`,
    Object.assign({}, params, { tags: { name: 'get_time_deals' } })
  );
}

/**
 * Purchase time deal product
 */
export function purchaseTimeDeal(params, timeDealProductId, quantity) {
  return http.post(
    `${BASE}/api/v1/shopping/time-deals/purchase`,
    JSON.stringify({ timeDealProductId, quantity: quantity || 1 }),
    Object.assign({}, params, { tags: { name: 'purchase_time_deal' } })
  );
}

/**
 * Issue a coupon
 */
export function issueCoupon(params, couponId) {
  return http.post(
    `${BASE}/api/v1/shopping/coupons/${couponId}/issue`,
    null,
    Object.assign({}, params, { tags: { name: 'coupon_issue' } })
  );
}

/**
 * Wait for CQRS sync — poll products until expectedCount is met
 */
export function waitForCQRSSync(params, expectedCount, maxRetries) {
  const retries = maxRetries || 30;
  for (let i = 0; i < retries; i++) {
    const res = getProducts(params, 1, 1);
    if (res.status === 200) {
      try {
        const total = res.json('data.totalElements') || res.json('data.total') || 0;
        if (total >= expectedCount) {
          console.log(`CQRS sync confirmed: ${total} products available (attempt ${i + 1})`);
          return true;
        }
      } catch (_) { /* retry */ }
    }
    sleep(2);
  }
  console.warn(`CQRS sync timeout after ${retries} retries`);
  return false;
}

/**
 * Get order by order number
 */
export function getOrder(params, orderNumber) {
  return http.get(
    `${BASE}/api/v1/shopping/orders/${orderNumber}`,
    Object.assign({}, params, { tags: { name: 'get_order' } })
  );
}
