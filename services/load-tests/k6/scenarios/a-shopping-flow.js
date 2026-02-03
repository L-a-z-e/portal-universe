import http from 'k6/http';
import { sleep } from 'k6';
import { login, authHeaders } from '../lib/auth.js';
import { config } from '../lib/config.js';
import { checkApiResponse } from '../lib/checks.js';

export const options = {
  scenarios: {
    shopping_flow: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '2m', target: 100 },
        { duration: '10m', target: 100 },
        { duration: '2m', target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<200', 'p(99)<500'],
    http_req_failed: ['rate<0.01'],
    'http_req_duration{name:product_list}': ['p(95)<100'],
    'http_req_duration{name:product_detail}': ['p(95)<80'],
    'http_req_duration{name:add_to_cart}': ['p(95)<120'],
  },
};

export function setup() {
  const token = login();
  if (!token) throw new Error('Setup failed: cannot login');
  return { token };
}

export default function (data) {
  const params = authHeaders(data.token);
  const base = config.BASE_URL;

  // 1. Product List (Gateway: /api/v1/shopping/**)
  const products = http.get(
    `${base}/api/v1/shopping/products?page=0&size=20`,
    Object.assign({}, params, { tags: { name: 'product_list' } })
  );
  checkApiResponse(products, 'product_list');
  sleep(1);

  // 2. Product Detail
  let productId;
  try {
    const content = products.json('data.content');
    if (content && content.length > 0) {
      productId = content[Math.floor(Math.random() * content.length)].id;
    }
  } catch (_) { /* ignore parse errors */ }

  if (productId) {
    const detail = http.get(
      `${base}/api/v1/shopping/products/${productId}`,
      Object.assign({}, params, { tags: { name: 'product_detail' } })
    );
    checkApiResponse(detail, 'product_detail');
    sleep(0.5);

    // 3. Add to Cart
    const cart = http.post(
      `${base}/api/v1/shopping/cart/items`,
      JSON.stringify({ productId, quantity: 1 }),
      Object.assign({}, params, { tags: { name: 'add_to_cart' } })
    );
    checkApiResponse(cart, 'add_to_cart');
  }

  sleep(1);
}
