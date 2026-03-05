/**
 * S4: 상품 조회 부하 (Browse Load - Read Heavy)
 *
 * 목적: 읽기 전용 조회 부하의 Baseline 측정 (캐싱 최적화 Before)
 * - 300명이 점진적으로 유입, 최대 500 VU까지 증가
 * - 가중치 기반 행동 분포: 목록 40%, 상세 30%, 검색 20%, 카테고리 10%
 * - DB 쿼리 부하 패턴 + 응답 시간 Baseline
 *
 * Portfolio Theme 2: 모니터링 기반 병목 발견 → 캐싱 최적화
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Counter, Rate } from 'k6/metrics';
import { loginBulk, getTokenForVU, authHeaders } from '../lib/auth.js';
import { config } from '../lib/config.js';
import {
  getProducts, getProductDetail, searchProducts,
  getProductsByCategory, getCategories, waitForCQRSSync,
} from '../lib/shopping-helpers.js';

const SEARCH_TERMS = [
  'Electronics', 'Premium', 'Item', 'quality', 'product',
  'high', 'description', 'price', 'special', 'sale',
];

// Custom metrics per operation type
const listDuration = new Trend('browse_list_duration');
const detailDuration = new Trend('browse_detail_duration');
const searchDuration = new Trend('browse_search_duration');
const categoryDuration = new Trend('browse_category_duration');
const browseErrors = new Counter('browse_errors');
const browseSuccessRate = new Rate('browse_success_rate');

export const options = {
  setupTimeout: '180s',
  scenarios: {
    browse_load: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m', target: 200 },   // ramp up
        { duration: '3m', target: 500 },   // ramp to peak
        { duration: '4m', target: 500 },   // sustained peak
        { duration: '1m', target: 200 },   // cool down
        { duration: '1m', target: 0 },     // drain
      ],
      gracefulRampDown: '30s',
    },
  },
  thresholds: {
    'http_req_duration{name:get_products}': ['p(95)<500', 'p(99)<1000'],
    'http_req_duration{name:product_detail}': ['p(95)<300', 'p(99)<800'],
    'http_req_duration{name:product_search}': ['p(95)<500', 'p(99)<1000'],
    'http_req_duration{name:product_by_category}': ['p(95)<500', 'p(99)<1000'],
    browse_success_rate: ['rate>0.95'],
  },
};

export function setup() {
  const tokens = loginBulk(300);
  if (tokens.length === 0) throw new Error('Setup failed: bulk login returned 0 tokens');

  const adminParams = authHeaders(tokens[0]);
  waitForCQRSSync(adminParams, 50, 20);

  // Fetch product IDs and categories for realistic browsing
  let productIds = [];
  let categories = [];

  const res = getProducts(adminParams, 1, 100);
  if (res.status === 200) {
    try {
      const items = res.json('data.items') || res.json('data.content') || [];
      productIds = items.map((p) => p.id || p.productId).filter(Boolean);
      const cats = items.map((p) => p.category).filter(Boolean);
      categories = [...new Set(cats)];
    } catch (_) { /* fallback */ }
  }

  if (productIds.length === 0) {
    for (let i = 1; i <= 50; i++) productIds.push(i);
  }

  // Try to get categories from API
  if (categories.length === 0) {
    const catRes = getCategories(adminParams);
    if (catRes.status === 200) {
      try {
        categories = catRes.json('data') || [];
      } catch (_) { /* fallback */ }
    }
  }
  if (categories.length === 0) {
    categories = ['Electronics', 'Clothing', 'Food'];
  }

  console.log(`Setup: ${tokens.length} tokens, ${productIds.length} products, ${categories.length} categories`);
  return { tokens, productIds, categories };
}

export default function (data) {
  const token = getTokenForVU(data.tokens);
  const params = authHeaders(token);

  // Weighted random action
  const roll = Math.random();
  let success = false;

  if (roll < 0.40) {
    // 40%: Product list browsing (pagination)
    const page = Math.floor(Math.random() * 5) + 1;
    const start = Date.now();
    const res = getProducts(params, page, 20);
    listDuration.add(Date.now() - start);
    success = check(res, { 'list ok': (r) => r.status === 200 });

  } else if (roll < 0.70) {
    // 30%: Product detail
    const pid = data.productIds[Math.floor(Math.random() * data.productIds.length)];
    const start = Date.now();
    const res = getProductDetail(params, pid);
    detailDuration.add(Date.now() - start);
    success = check(res, { 'detail ok': (r) => r.status === 200 });

  } else if (roll < 0.90) {
    // 20%: Search
    const term = SEARCH_TERMS[Math.floor(Math.random() * SEARCH_TERMS.length)];
    const start = Date.now();
    const res = searchProducts(params, term, 0, 20);
    searchDuration.add(Date.now() - start);
    success = check(res, { 'search ok': (r) => r.status === 200 });

  } else {
    // 10%: Category browsing
    const cat = data.categories[Math.floor(Math.random() * data.categories.length)];
    const start = Date.now();
    const res = getProductsByCategory(params, cat, 1, 20);
    categoryDuration.add(Date.now() - start);
    success = check(res, { 'category ok': (r) => r.status === 200 });
  }

  if (!success) {
    browseErrors.add(1);
  }
  browseSuccessRate.add(success);

  sleep(0.5 + Math.random() * 1.5); // realistic browse think time
}
