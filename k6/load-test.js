import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');
const productSearchTrend = new Trend('product_search_duration');
const authTrend = new Trend('auth_duration');

// Configuration
export const options = {
  scenarios: {
    // Smoke Test - Basic functionality check
    smoke: {
      executor: 'constant-vus',
      vus: 5,
      duration: '1m',
      tags: { scenario: 'smoke' },
      startTime: '0s',
    },
    // Load Test - Expected traffic
    load: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '2m', target: 100 },   // ramp up
        { duration: '5m', target: 100 },   // stay at 100
        { duration: '2m', target: 500 },   // ramp up
        { duration: '5m', target: 500 },   // stay at 500
        { duration: '2m', target: 1000 },  // ramp up to peak
        { duration: '5m', target: 1000 },  // stay at peak
        { duration: '2m', target: 0 },     // ramp down
      ],
      tags: { scenario: 'load' },
      startTime: '1m',  // Start after smoke test
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<500', 'p(99)<1000'],
    http_req_failed: ['rate<0.01'],
    errors: ['rate<0.01'],
    product_search_duration: ['p(95)<300'],
    auth_duration: ['p(95)<200'],
  },
};

const BASE_URL = __ENV.K6_TARGET_URL || 'http://localhost:8080';

// Test data
const testUsers = [
  { email: 'loadtest1@example.com', password: 'Test1234!' },
  { email: 'loadtest2@example.com', password: 'Test1234!' },
  { email: 'loadtest3@example.com', password: 'Test1234!' },
];

const searchKeywords = ['노트북', '스마트폰', '헤드폰', '키보드', '모니터', '태블릿'];

// Setup - Login and get tokens
export function setup() {
  const tokens = [];

  for (const user of testUsers) {
    const startTime = Date.now();
    const res = http.post(`${BASE_URL}/api/v1/auth/login`, JSON.stringify(user), {
      headers: { 'Content-Type': 'application/json' },
    });
    authTrend.add(Date.now() - startTime);

    if (res.status === 200) {
      try {
        const data = JSON.parse(res.body);
        if (data.data && data.data.accessToken) {
          tokens.push(data.data.accessToken);
        }
      } catch (e) {
        console.log(`Failed to parse login response for ${user.email}`);
      }
    }
  }

  // If no tokens, use a dummy for testing
  if (tokens.length === 0) {
    console.log('Warning: No tokens obtained, using public endpoints only');
  }

  return { tokens };
}

// Main test function
export default function(data) {
  const token = data.tokens.length > 0
    ? data.tokens[Math.floor(Math.random() * data.tokens.length)]
    : null;

  const headers = {
    'Content-Type': 'application/json',
    ...(token && { 'Authorization': `Bearer ${token}` }),
  };

  // Test Product Listing
  group('Product Listing', () => {
    const res = http.get(`${BASE_URL}/api/v1/shopping/products?page=0&size=20`, { headers });

    const success = check(res, {
      'products list status 200': (r) => r.status === 200,
      'products list has data': (r) => {
        try {
          const body = JSON.parse(r.body);
          return body.data && body.data.content && body.data.content.length > 0;
        } catch (e) {
          return false;
        }
      },
    });

    if (!success) {
      errorRate.add(1);
    }

    sleep(0.5);
  });

  // Test Product Detail
  group('Product Detail', () => {
    const productId = Math.floor(Math.random() * 100) + 1;
    const res = http.get(`${BASE_URL}/api/v1/shopping/products/${productId}`, { headers });

    const success = check(res, {
      'product detail status 200 or 404': (r) => r.status === 200 || r.status === 404,
    });

    if (!success) {
      errorRate.add(1);
    }

    sleep(0.3);
  });

  // Test Product Search
  group('Product Search', () => {
    const keyword = searchKeywords[Math.floor(Math.random() * searchKeywords.length)];
    const startTime = Date.now();

    const res = http.get(`${BASE_URL}/api/v1/shopping/search/products?keyword=${encodeURIComponent(keyword)}&size=20`, { headers });

    productSearchTrend.add(Date.now() - startTime);

    const success = check(res, {
      'search status 200': (r) => r.status === 200,
    });

    if (!success) {
      errorRate.add(1);
    }

    sleep(0.5);
  });

  // Test Cart Operations (only if authenticated)
  if (token) {
    group('Cart Operations', () => {
      // Get cart
      let res = http.get(`${BASE_URL}/api/v1/shopping/cart`, { headers });

      check(res, {
        'cart status 200': (r) => r.status === 200,
      }) || errorRate.add(1);

      // Add to cart
      const productId = Math.floor(Math.random() * 100) + 1;
      res = http.post(`${BASE_URL}/api/v1/shopping/cart/items`, JSON.stringify({
        productId: productId,
        quantity: 1,
      }), { headers });

      check(res, {
        'add to cart status 200 or 409': (r) => r.status === 200 || r.status === 409 || r.status === 404,
      }) || errorRate.add(1);

      sleep(1);
    });
  }

  // Test Time Deals (public endpoint)
  group('Time Deals', () => {
    const res = http.get(`${BASE_URL}/api/v1/shopping/time-deals`, { headers });

    check(res, {
      'time deals status 200': (r) => r.status === 200,
    }) || errorRate.add(1);

    sleep(0.3);
  });

  // Test Coupons (authenticated)
  if (token) {
    group('Coupons', () => {
      const res = http.get(`${BASE_URL}/api/v1/shopping/coupons`, { headers });

      check(res, {
        'coupons status 200': (r) => r.status === 200,
      }) || errorRate.add(1);

      sleep(0.5);
    });
  }

  sleep(Math.random() * 2 + 1); // Random think time between 1-3 seconds
}

// Teardown
export function teardown(data) {
  console.log('Load test completed');
  console.log(`Tokens used: ${data.tokens.length}`);
}

// Handle summary
export function handleSummary(data) {
  return {
    'stdout': textSummary(data, { indent: ' ', enableColors: true }),
    'results.json': JSON.stringify(data, null, 2),
  };
}

function textSummary(data, options) {
  const metrics = data.metrics;
  let summary = '\n=== Load Test Summary ===\n\n';

  // HTTP metrics
  if (metrics.http_req_duration) {
    const duration = metrics.http_req_duration.values;
    summary += `HTTP Request Duration:\n`;
    summary += `  avg: ${duration.avg.toFixed(2)}ms\n`;
    summary += `  p95: ${duration['p(95)'].toFixed(2)}ms\n`;
    summary += `  p99: ${duration['p(99)'].toFixed(2)}ms\n\n`;
  }

  if (metrics.http_reqs) {
    summary += `Total Requests: ${metrics.http_reqs.values.count}\n`;
    summary += `RPS: ${metrics.http_reqs.values.rate.toFixed(2)}\n\n`;
  }

  if (metrics.http_req_failed) {
    const failRate = metrics.http_req_failed.values.rate * 100;
    summary += `Error Rate: ${failRate.toFixed(2)}%\n\n`;
  }

  // Custom metrics
  if (metrics.product_search_duration) {
    summary += `Product Search Duration (p95): ${metrics.product_search_duration.values['p(95)'].toFixed(2)}ms\n`;
  }

  if (metrics.auth_duration) {
    summary += `Auth Duration (p95): ${metrics.auth_duration.values['p(95)'].toFixed(2)}ms\n`;
  }

  return summary;
}
