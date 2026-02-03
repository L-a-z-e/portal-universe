import http from 'k6/http';
import { config } from './config.js';

const TEST_EMAIL = __ENV.TEST_EMAIL || 'test@example.com';
const TEST_PASSWORD = __ENV.TEST_PASSWORD || 'password123';

export function login(email, password) {
  const res = http.post(`${config.BASE_URL}/api/v1/auth/login`,
    JSON.stringify({ email: email || TEST_EMAIL, password: password || TEST_PASSWORD }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  if (res.status !== 200) {
    console.error(`Login failed: ${res.status} ${res.body}`);
    return null;
  }

  return res.json('data.accessToken');
}

export function authHeaders(token) {
  return {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    },
  };
}
