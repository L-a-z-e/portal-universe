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

/**
 * Check that response status is one of the valid statuses (for contention scenarios)
 */
export function checkValidStatus(res, name, validStatuses) {
  return check(res, {
    [`${name} status valid`]: (r) => validStatuses.includes(r.status),
  });
}

/**
 * Check Saga order flow response — success or expected contention error
 */
export function checkOrderResponse(res, name) {
  return check(res, {
    [`${name} status valid`]: (r) => [200, 201, 400, 409, 422, 429, 503].includes(r.status),
    [`${name} success or contention`]: (r) => r.status === 200 || r.status === 201 || r.status === 409,
  });
}
