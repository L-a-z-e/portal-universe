// portal-shell/src/store/serviceStatus.ts
// Service health status store — fetches from /api/health/services

import { defineStore } from 'pinia';

export type ServiceStatus = 'up' | 'down' | 'degraded' | 'unknown';

export interface PodInfo {
  name: string;
  phase: string;
  ready: boolean;
  restarts: number;
}

export interface ServiceHealth {
  name: string;
  displayName: string;
  status: ServiceStatus;
  responseTime?: number;
  lastChecked: Date | null;
  error?: string;
  replicas?: number;
  readyReplicas?: number;
  pods?: PodInfo[];
}

export interface ServiceStatusState {
  services: Record<string, ServiceHealth>;
  isPolling: boolean;
  pollInterval: number;
  lastGlobalCheck: Date | null;
}

const DEFAULT_POLL_INTERVAL = 10000;
const TIMEOUT_MS = 8000;

export const useServiceStatusStore = defineStore('serviceStatus', {
  state: (): ServiceStatusState => ({
    services: {},
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
      if (services.length === 0) return 'unknown';
      if (services.every((s) => s.status === 'up')) return 'up';
      if (services.every((s) => s.status === 'down')) return 'down';
      if (services.some((s) => s.status === 'down' || s.status === 'degraded')) return 'degraded';
      return 'unknown';
    },

    getService: (state) => (key: string) => state.services[key],
  },

  actions: {
    async checkAllServices(): Promise<void> {
      const baseUrl = import.meta.env.VITE_API_BASE_URL || '';

      try {
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), TIMEOUT_MS);

        const response = await fetch(`${baseUrl}/api/health/services`, {
          method: 'GET',
          signal: controller.signal,
          headers: { Accept: 'application/json' },
        });

        clearTimeout(timeoutId);

        if (!response.ok) {
          this.markAllDown(`HTTP ${response.status}`);
          return;
        }

        const data = await response.json();
        const now = new Date();

        const updatedServices: Record<string, ServiceHealth> = {};
        for (const svc of data.services ?? []) {
          updatedServices[svc.name] = {
            name: svc.name,
            displayName: svc.displayName,
            status: (svc.status as ServiceStatus) ?? 'unknown',
            responseTime: svc.responseTime,
            lastChecked: now,
            replicas: svc.replicas ?? undefined,
            readyReplicas: svc.readyReplicas ?? undefined,
            pods: svc.pods ?? undefined,
          };
        }

        this.services = updatedServices;
      } catch (error) {
        const msg = error instanceof Error
          ? (error.name === 'AbortError' ? 'Timeout' : error.message)
          : 'Unknown error';
        this.markAllDown(msg);
      }

      this.lastGlobalCheck = new Date();
    },

    markAllDown(errorMessage: string) {
      const now = new Date();
      for (const key of Object.keys(this.services)) {
        const service = this.services[key];
        if (!service) continue;
        service.status = 'down';
        service.error = errorMessage;
        service.lastChecked = now;
      }
    },

    setPollInterval(interval: number): void {
      this.pollInterval = Math.max(5000, Math.min(60000, interval));
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
