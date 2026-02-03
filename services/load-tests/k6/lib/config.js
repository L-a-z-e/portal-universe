const ENV = __ENV.TARGET_ENV || 'local';

const configs = {
  local: {
    BASE_URL: 'http://localhost:8080',
  },
  docker: {
    BASE_URL: 'http://host.docker.internal:8080',
  },
};

export const config = configs[ENV] || configs.local;
