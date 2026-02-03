import { check } from 'k6';

export function checkApiResponse(res, name) {
  return check(res, {
    [`${name} status 200`]: (r) => r.status === 200,
    [`${name} success`]: (r) => {
      try { return r.json('success') === true; } catch (e) { return false; }
    },
  });
}

export function checkStatus(res, name, expectedStatus = 200) {
  return check(res, {
    [`${name} status ${expectedStatus}`]: (r) => r.status === expectedStatus,
  });
}
