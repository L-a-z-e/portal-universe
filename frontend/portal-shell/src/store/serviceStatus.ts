// portal-shell/src/store/serviceStatus.ts
// Service health status store with polling

import { defineStore } from 'pinia';

export type ServiceStatus = 'up' | 'down' | 'degraded' | 'unknown';

export interface ServiceHealth {
  name: string;
  displayName: string;
  status: ServiceStatus;
  responseTime?: number;
  lastChecked: Date | null;
  error?: string;
  details?: Record<string, unknown>;
}

export interface ServiceStatusState {
  services: Record<string, ServiceHealth>;
  isPolling: boolean;
  pollInterval: number; // in milliseconds
  lastGlobalCheck: Date | null;
}

// Service configurations
const SERVICE_CONFIGS: { key: string; displayName: string; healthUrl: string }[] = [
  {
    key: 'api-gateway',
    displayName: 'API Gateway',
    healthUrl: '/actuator/health',
  },
  {
    key: 'auth-service',
    displayName: 'Auth Service',
    healthUrl: '/api/v1/auth/actuator/health',
  },
  {
    key: 'blog-service',
    displayName: 'Blog Service',
    healthUrl: '/api/v1/blog/actuator/health',
  },
  {
    key: 'shopping-service',
    displayName: 'Shopping Service',
    healthUrl: '/api/v1/shopping/actuator/health',
  },
];

const TIMEOUT_MS = 5000;
const DEFAULT_POLL_INTERVAL = 10000; // 10 seconds

function createInitialServices(): Record<string, ServiceHealth> {
  const services: Record<string, ServiceHealth> = {};
  for (const config of SERVICE_CONFIGS) {
    services[config.key] = {
      name: config.key,
      displayName: config.displayName,
      status: 'unknown',
      lastChecked: null,
    };
  }
  return services;
}

export const useServiceStatusStore = defineStore('serviceStatus', {
  state: (): ServiceStatusState => ({
    services: createInitialServices(),
    isPolling: false,
    pollInterval: DEFAULT_POLL_INTERVAL,
    lastGlobalCheck: null,
  }),

  getters: {
    allServices: (state) => Object.values(state.services),

    healthyServices: (state) =>
      Object.values(state.services).filter((s) => s.status === 'up'),

    unhealthyServices: (state) =>
      Object.values(state.services).filter((s) => s.status === 'down'),

    overallStatus: (state): ServiceStatus => {
      const services = Object.values(state.services);
      if (services.every((s) => s.status === 'up')) return 'up';
      if (services.every((s) => s.status === 'down')) return 'down';
      if (services.some((s) => s.status === 'down' || s.status === 'degraded')) return 'degraded';
      return 'unknown';
    },

    getService: (state) => (key: string) => state.services[key],
  },

  actions: {
    async checkServiceHealth(serviceKey: string): Promise<void> {
      const config = SERVICE_CONFIGS.find((c) => c.key === serviceKey);
      if (!config) return;

      const service = this.services[serviceKey];
      if (!service) return;

      const startTime = Date.now();
      const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

      try {
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), TIMEOUT_MS);

        const response = await fetch(`${baseUrl}${config.healthUrl}`, {
          method: 'GET',
          signal: controller.signal,
          headers: {
            Accept: 'application/json',
          },
        });

        clearTimeout(timeoutId);
        const responseTime = Date.now() - startTime;

        if (response.ok) {
          const data = await response.json();
          service.status = data.status?.toLowerCase() === 'up' ? 'up' : 'degraded';
          service.details = data;
          service.error = undefined;
        } else {
          service.status = response.status >= 500 ? 'down' : 'degraded';
          service.error = `HTTP ${response.status}`;
        }

        service.responseTime = responseTime;
      } catch (error) {
        service.status = 'down';
        service.responseTime = Date.now() - startTime;

        if (error instanceof Error) {
          if (error.name === 'AbortError') {
            service.error = 'Timeout';
          } else {
            service.error = error.message;
          }
        } else {
          service.error = 'Unknown error';
        }
      }

      service.lastChecked = new Date();
    },

    async checkAllServices(): Promise<void> {
      const checks = SERVICE_CONFIGS.map((config) => this.checkServiceHealth(config.key));
      await Promise.allSettled(checks);
      this.lastGlobalCheck = new Date();
    },

    setPollInterval(interval: number): void {
      this.pollInterval = Math.max(5000, Math.min(60000, interval)); // 5s to 60s
    },

    startPolling(): void {
      if (this.isPolling) return;
      this.isPolling = true;
    },

    stopPolling(): void {
      this.isPolling = false;
    },
  },
});
