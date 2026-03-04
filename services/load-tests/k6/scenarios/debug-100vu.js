import http from 'k6/http';
import { Counter } from 'k6/metrics';
import { login, authHeaders } from '../lib/auth.js';
import { config } from '../lib/config.js';

const status200 = new Counter('status_200');
const status429 = new Counter('status_429');
const status502 = new Counter('status_502');
const status503 = new Counter('status_503');
const statusOther = new Counter('status_other');

export const options = { vus: 100, duration: '15s' };

export function setup() {
  const token = login();
  if (!token) throw new Error('Login failed');
  return { token };
}

let printed = 0;
export default function (data) {
  const params = authHeaders(data.token);
  const res = http.get(
    `${config.BASE_URL}/api/v1/blog/posts?page=1&size=10`,
    Object.assign({}, params, { tags: { name: 'post_list' } })
  );

  if (res.status === 200) status200.add(1);
  else if (res.status === 429) status429.add(1);
  else if (res.status === 502) status502.add(1);
  else if (res.status === 503) {
    status503.add(1);
    if (printed < 2) {
      printed++;
      console.log(`503 body: ${res.body ? res.body.substring(0, 300) : 'empty'}`);
    }
  } else {
    statusOther.add(1);
    if (printed < 5) {
      printed++;
      console.log(`Status ${res.status}: ${res.body ? res.body.substring(0, 200) : 'empty'}`);
    }
  }
}
