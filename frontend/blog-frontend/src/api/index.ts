import axios from 'axios';

const apiClient = axios.create({
  baseURL: 'http://localhost:8080'
});

apiClient.interceptors.request.use(
  (config) => {
    const oidcStorage = localStorage.getItem('oidc.user:http://localhost:8081:portal-client');

    if (oidcStorage) {
      const user = JSON.parse(oidcStorage);
      const token = user.access_token;
      console.log('Token', token);

      if (token) {
        console.log('Adding Authorization header with token:', token);
        config.headers.Authorization = `Bearer ${token}`;
      }
    }

    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

export default apiClient;