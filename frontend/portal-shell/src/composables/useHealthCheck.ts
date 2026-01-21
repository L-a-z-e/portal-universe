// portal-shell/src/composables/useHealthCheck.ts
// Health check polling composable

import { ref, onMounted, onUnmounted, computed } from 'vue';
import { useServiceStatusStore } from '../store/serviceStatus';

export interface UseHealthCheckOptions {
  autoStart?: boolean;
  pollInterval?: number;
}

export function useHealthCheck(options: UseHealthCheckOptions = {}) {
  const { autoStart = true, pollInterval = 10000 } = options;

  const store = useServiceStatusStore();
  const intervalId = ref<number | null>(null);
  const isLoading = ref(false);

  // Computed from store
  const services = computed(() => store.allServices);
  const healthyServices = computed(() => store.healthyServices);
  const unhealthyServices = computed(() => store.unhealthyServices);
  const overallStatus = computed(() => store.overallStatus);
  const lastChecked = computed(() => store.lastGlobalCheck);
  const isPolling = computed(() => store.isPolling);

  // Manual refresh
  async function refresh() {
    isLoading.value = true;
    try {
      await store.checkAllServices();
    } finally {
      isLoading.value = false;
    }
  }

  // Start polling
  function startPolling() {
    if (intervalId.value !== null) return;

    store.startPolling();

    // Initial check
    refresh();

    // Set up interval
    intervalId.value = window.setInterval(() => {
      if (store.isPolling) {
        refresh();
      }
    }, pollInterval);
  }

  // Stop polling
  function stopPolling() {
    store.stopPolling();

    if (intervalId.value !== null) {
      clearInterval(intervalId.value);
      intervalId.value = null;
    }
  }

  // Set poll interval
  function setPollInterval(interval: number) {
    store.setPollInterval(interval);

    // Restart polling with new interval if currently polling
    if (store.isPolling) {
      stopPolling();
      startPolling();
    }
  }

  // Lifecycle
  onMounted(() => {
    if (autoStart) {
      startPolling();
    }
  });

  onUnmounted(() => {
    stopPolling();
  });

  return {
    // State
    services,
    healthyServices,
    unhealthyServices,
    overallStatus,
    lastChecked,
    isPolling,
    isLoading,

    // Actions
    refresh,
    startPolling,
    stopPolling,
    setPollInterval,
  };
}
