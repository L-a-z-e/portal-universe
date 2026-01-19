import http from 'k6/http';
import { check } from 'k6';
import { Counter, Trend } from 'k6/metrics';

// Custom metrics
const successCount = new Counter('coupon_success');
const failCount = new Counter('coupon_fail');
const alreadyIssuedCount = new Counter('coupon_already_issued');
const exhaustedCount = new Counter('coupon_exhausted');
const issueTrend = new Trend('coupon_issue_duration');

// Stress test configuration - simulates flash sale scenario
export const options = {
  scenarios: {
    coupon_rush: {
      executor: 'shared-iterations',
      vus: 1000,              // 1000 concurrent users
      iterations: 10000,      // Total 10000 attempts
      maxDuration: '2m',
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<1000'],  // Under load, allow up to 1s
    http_req_failed: ['rate<0.05'],      // Allow up to 5% failures (expected for exhausted coupons)
  },
};

const BASE_URL = __ENV.K6_TARGET_URL || 'http://localhost:8080';
const COUPON_ID = __ENV.COUPON_ID || '1';

// Generate unique user IDs for testing
export function setup() {
  const tokens = [];

  // In a real scenario, you would have pre-generated test tokens
  // For now, we'll use X-User-Id header simulation
  for (let i = 1; i <= 1000; i++) {
    tokens.push({
      userId: i,
      token: `test-token-${i}`,  // Placeholder - replace with real tokens
    });
  }

  console.log(`Setup complete: ${tokens.length} test users ready`);
  console.log(`Target coupon ID: ${COUPON_ID}`);

  return { tokens };
}

export default function(data) {
  // Pick a random user for this iteration
  const userIndex = Math.floor(Math.random() * data.tokens.length);
  const user = data.tokens[userIndex];

  const headers = {
    'Content-Type': 'application/json',
    'X-User-Id': String(user.userId),
  };

  // If you have real tokens, use them:
  // headers['Authorization'] = `Bearer ${user.token}`;

  const startTime = Date.now();

  const res = http.post(
    `${BASE_URL}/api/v1/shopping/coupons/${COUPON_ID}/issue`,
    null,
    { headers }
  );

  issueTrend.add(Date.now() - startTime);

  // Check response and categorize
  if (res.status === 200) {
    successCount.add(1);
    check(res, {
      'coupon issued successfully': (r) => r.status === 200,
    });
  } else if (res.status === 409) {
    // Conflict - could be already issued or exhausted
    try {
      const body = JSON.parse(res.body);
      if (body.code === 'S604') {
        alreadyIssuedCount.add(1);
      } else if (body.code === 'S602') {
        exhaustedCount.add(1);
      } else {
        failCount.add(1);
      }
    } catch (e) {
      failCount.add(1);
    }

    check(res, {
      'expected conflict response': (r) => r.status === 409,
    });
  } else {
    failCount.add(1);
    check(res, {
      'unexpected status': (r) => false,
    });
  }
}

export function handleSummary(data) {
  const success = data.metrics.coupon_success?.values?.count || 0;
  const fail = data.metrics.coupon_fail?.values?.count || 0;
  const alreadyIssued = data.metrics.coupon_already_issued?.values?.count || 0;
  const exhausted = data.metrics.coupon_exhausted?.values?.count || 0;
  const total = success + fail + alreadyIssued + exhausted;

  const avgDuration = data.metrics.coupon_issue_duration?.values?.avg || 0;
  const p95Duration = data.metrics.coupon_issue_duration?.values?.['p(95)'] || 0;
  const p99Duration = data.metrics.coupon_issue_duration?.values?.['p(99)'] || 0;

  const summary = `
=== Coupon Stress Test Results ===

Requests:
  Total:          ${total}
  Successful:     ${success} (${((success / total) * 100).toFixed(2)}%)
  Already Issued: ${alreadyIssued} (${((alreadyIssued / total) * 100).toFixed(2)}%)
  Exhausted:      ${exhausted} (${((exhausted / total) * 100).toFixed(2)}%)
  Failed:         ${fail} (${((fail / total) * 100).toFixed(2)}%)

Performance:
  Avg Duration:   ${avgDuration.toFixed(2)}ms
  p95 Duration:   ${p95Duration.toFixed(2)}ms
  p99 Duration:   ${p99Duration.toFixed(2)}ms

Verification:
  ${success <= 1000 ? '✅' : '❌'} Successful issues should not exceed coupon quantity
`;

  console.log(summary);

  return {
    'stdout': summary,
    'coupon-stress-results.json': JSON.stringify({
      success,
      fail,
      alreadyIssued,
      exhausted,
      total,
      performance: {
        avgDuration,
        p95Duration,
        p99Duration,
      },
      passed: success <= 1000 && fail === 0,
    }, null, 2),
  };
}
