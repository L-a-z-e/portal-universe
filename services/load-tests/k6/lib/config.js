const ENV = __ENV.TARGET_ENV || 'local';

const configs = {
  local: {
    BASE_URL: 'http://localhost:8080',
    HOST_HEADER: null,
    DEFAULT_VU_ACCOUNTS: 100,
  },
  docker: {
    BASE_URL: 'http://host.docker.internal:8080',
    HOST_HEADER: null,
    DEFAULT_VU_ACCOUNTS: 100,
  },
  k8s: {
    BASE_URL: 'http://localhost:80',
    HOST_HEADER: 'portal-universe',
    DEFAULT_VU_ACCOUNTS: 500,
  },
  'k8s-pod': {
    BASE_URL: 'http://api-gateway.portal-universe.svc:8080',
    HOST_HEADER: null,
    DEFAULT_VU_ACCOUNTS: 500,
  },
};

export const config = configs[ENV] || configs.local;
