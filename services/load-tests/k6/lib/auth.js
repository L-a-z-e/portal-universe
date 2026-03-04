import http from 'k6/http';
import { SharedArray } from 'k6/data';
import { config } from './config.js';

const TEST_EMAIL = __ENV.TEST_EMAIL || 'test@test.com';
const TEST_PASSWORD = __ENV.TEST_PASSWORD || 'test1234';

// Bulk 계정 목록 (SharedArray: init context에서 1회 생성, VU 간 메모리 공유)
const BULK_ACCOUNTS = new SharedArray('bulk-accounts', function () {
  const count = parseInt(__ENV.VU_ACCOUNTS || config.DEFAULT_VU_ACCOUNTS || '100');
  const accounts = [];
  for (let i = 0; i < count; i++) {
    accounts.push({
      email: `testaccount${String(i).padStart(4, '0')}@test.com`,
      password: 'test1234',
    });
  }
  return accounts;
});

function baseHeaders() {
  const h = { 'Content-Type': 'application/json' };
  if (config.HOST_HEADER) h['Host'] = config.HOST_HEADER;
  return h;
}

/**
 * 단일 계정 로그인 (기존 호환)
 */
export function login(email, password) {
  const res = http.post(`${config.BASE_URL}/api/v1/auth/login`,
    JSON.stringify({ email: email || TEST_EMAIL, password: password || TEST_PASSWORD }),
    { headers: baseHeaders(), tags: { name: 'login' } }
  );

  if (res.status !== 200) {
    console.error(`Login failed: ${res.status} ${res.body}`);
    return null;
  }

  return res.json('data.accessToken');
}

/**
 * Bulk 계정 로그인 — 50개씩 http.batch()로 병렬 로그인
 * @param {number} [count] - 로그인할 계정 수 (기본: VU_ACCOUNTS 또는 config)
 * @returns {string[]} 토큰 배열
 */
export function loginBulk(count) {
  const total = count || BULK_ACCOUNTS.length;
  const batchSize = 50;
  const tokens = [];

  for (let offset = 0; offset < total; offset += batchSize) {
    const end = Math.min(offset + batchSize, total);
    const requests = [];

    for (let i = offset; i < end; i++) {
      const account = BULK_ACCOUNTS[i];
      requests.push([
        'POST',
        `${config.BASE_URL}/api/v1/auth/login`,
        JSON.stringify({ email: account.email, password: account.password }),
        { headers: baseHeaders(), tags: { name: 'login' } },
      ]);
    }

    const responses = http.batch(requests);

    for (const res of responses) {
      if (res.status === 200) {
        try {
          tokens.push(res.json('data.accessToken'));
        } catch (_) {
          console.warn('Failed to parse token from bulk login response');
        }
      } else {
        console.warn(`Bulk login failed: ${res.status}`);
      }
    }
  }

  console.log(`Bulk login completed: ${tokens.length}/${total} tokens acquired`);
  return tokens;
}

/**
 * VU별 고유 토큰 분배
 * @param {string[]} tokens - loginBulk()에서 반환된 토큰 배열
 * @returns {string} 현재 VU에 할당된 토큰
 */
export function getTokenForVU(tokens) {
  return tokens[(__VU - 1) % tokens.length];
}

export function authHeaders(token) {
  const h = Object.assign({}, baseHeaders(), { 'Authorization': `Bearer ${token}` });
  return { headers: h };
}
