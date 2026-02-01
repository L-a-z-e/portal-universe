<script setup lang="ts">
import { ref, computed } from 'vue';
import { Button, Switch } from '@portal/design-system-vue';
import { useHealthCheck } from '../composables/useHealthCheck';
import type { ServiceStatus, ServiceHealth } from '../store/serviceStatus';

const { services, overallStatus, lastChecked, isLoading, refresh, startPolling, stopPolling } = useHealthCheck();

// Pod detail expansion state
const expandedService = ref<string | null>(null);

const togglePods = (serviceName: string) => {
  expandedService.value = expandedService.value === serviceName ? null : serviceName;
};

// Format time
const formatTime = (date: Date | null): string => {
  if (!date) return 'Never';
  return date.toLocaleTimeString('ko-KR', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  });
};

// Format response time
const formatResponseTime = (ms?: number): string => {
  if (ms === undefined) return '-';
  if (ms < 1000) return `${ms}ms`;
  return `${(ms / 1000).toFixed(1)}s`;
};

// Status config
const statusConfig: Record<ServiceStatus, { icon: string; color: string; label: string }> = {
  up: { icon: '🟢', color: 'text-status-success', label: 'Healthy' },
  down: { icon: '🔴', color: 'text-status-error', label: 'Down' },
  degraded: { icon: '🟡', color: 'text-status-warning', label: 'Degraded' },
  unknown: { icon: '⚪', color: 'text-text-meta', label: 'Unknown' },
};

// Overall status message
const overallMessage = computed(() => {
  switch (overallStatus.value) {
    case 'up':
      return 'All systems operational';
    case 'down':
      return 'Major outage detected';
    case 'degraded':
      return 'Some systems experiencing issues';
    default:
      return 'Checking systems...';
  }
});

// Check if any service has K8s info
const hasK8sInfo = computed(() => services.value.some((s: ServiceHealth) => s.replicas !== undefined));

// Auto refresh toggle
const autoRefresh = ref(true);

const toggleAutoRefresh = (value: boolean) => {
  if (value) {
    startPolling();
  } else {
    stopPolling();
  }
};
</script>

<template>
  <div class="max-w-4xl mx-auto px-4 py-8">
    <!-- Header -->
    <div class="mb-8">
      <h1 class="text-2xl font-bold text-text-heading mb-2">Service Status</h1>
      <p class="text-text-meta">Monitor the health of Portal Universe services</p>
    </div>

    <!-- Overall Status Banner -->
    <div
      :class="[
        'rounded-xl p-6 mb-8 border',
        overallStatus === 'up' ? 'bg-status-success/10 border-status-success/30' :
        overallStatus === 'down' ? 'bg-status-error/10 border-status-error/30' :
        overallStatus === 'degraded' ? 'bg-status-warning/10 border-status-warning/30' :
        'bg-bg-elevated border-border-default'
      ]"
    >
      <div class="flex items-center justify-between flex-wrap gap-4">
        <div class="flex items-center gap-4">
          <span class="text-4xl">{{ statusConfig[overallStatus].icon }}</span>
          <div>
            <h2 :class="['text-xl font-semibold', statusConfig[overallStatus].color]">
              {{ overallMessage }}
            </h2>
            <p class="text-sm text-text-meta">
              Last checked: {{ formatTime(lastChecked) }}
            </p>
          </div>
        </div>

        <div class="flex items-center gap-3">
          <!-- Auto Refresh Toggle -->
          <Switch v-model="autoRefresh" label="Auto Refresh" @update:model-value="toggleAutoRefresh" />

          <!-- Manual Refresh Button -->
          <Button
            variant="primary"
            size="sm"
            :loading="isLoading"
            :disabled="isLoading"
            @click="refresh"
          >
            Refresh
          </Button>
        </div>
      </div>
    </div>

    <!-- Services Grid -->
    <div class="grid gap-4 md:grid-cols-2">
      <div
        v-for="service in services"
        :key="service.name"
        class="bg-bg-card rounded-xl border border-border-default p-5 hover:border-border-hover transition-colors"
      >
        <div class="flex items-start justify-between mb-3">
          <div class="flex items-center gap-3">
            <span class="text-2xl">{{ statusConfig[service.status].icon }}</span>
            <div>
              <h3 class="font-semibold text-text-heading">{{ service.displayName }}</h3>
              <p class="text-xs text-text-meta">{{ service.name }}</p>
            </div>
          </div>
          <span
            :class="[
              'px-2 py-1 rounded-md text-xs font-medium',
              service.status === 'up' ? 'bg-status-success/10 text-status-success' :
              service.status === 'down' ? 'bg-status-error/10 text-status-error' :
              service.status === 'degraded' ? 'bg-status-warning/10 text-status-warning' :
              'bg-bg-elevated text-text-meta'
            ]"
          >
            {{ statusConfig[service.status].label }}
          </span>
        </div>

        <div class="space-y-2 text-sm">
          <!-- Response Time -->
          <div class="flex justify-between items-center">
            <span class="text-text-meta">Response Time</span>
            <span
              :class="[
                'font-mono',
                service.responseTime && service.responseTime > 1000
                  ? 'text-status-warning'
                  : 'text-text-body'
              ]"
            >
              {{ formatResponseTime(service.responseTime) }}
            </span>
          </div>

          <!-- Replicas (K8s only) -->
          <div v-if="service.replicas !== undefined" class="flex justify-between items-center">
            <span class="text-text-meta">Replicas</span>
            <span
              :class="[
                'font-mono',
                service.readyReplicas !== undefined && service.readyReplicas < service.replicas
                  ? 'text-status-warning'
                  : 'text-text-body'
              ]"
            >
              {{ service.readyReplicas ?? 0 }} / {{ service.replicas }}
              <span v-if="service.readyReplicas !== undefined && service.readyReplicas < service.replicas" class="ml-1">⚠️</span>
            </span>
          </div>

          <!-- Last Checked -->
          <div class="flex justify-between items-center">
            <span class="text-text-meta">Last Checked</span>
            <span class="text-text-body">{{ formatTime(service.lastChecked) }}</span>
          </div>

          <!-- Error (if any) -->
          <div v-if="service.error" class="mt-2 p-2 bg-status-error/10 rounded-lg">
            <p class="text-xs text-status-error font-mono">{{ service.error }}</p>
          </div>

          <!-- Pods (K8s only, collapsible) -->
          <div v-if="service.pods && service.pods.length > 0" class="mt-2">
            <button
              @click="togglePods(service.name)"
              class="flex items-center gap-1 text-xs text-text-meta hover:text-text-body transition-colors"
            >
              <svg
                :class="['w-3 h-3 transition-transform', expandedService === service.name ? 'rotate-90' : '']"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
              </svg>
              <span>{{ service.pods.length }} Pod{{ service.pods.length > 1 ? 's' : '' }}</span>
            </button>

            <div v-if="expandedService === service.name" class="mt-2 space-y-1">
              <div
                v-for="pod in service.pods"
                :key="pod.name"
                class="flex items-center justify-between p-2 bg-bg-elevated rounded-lg text-xs"
              >
                <div class="flex items-center gap-2 min-w-0">
                  <span :class="pod.ready ? 'text-status-success' : 'text-status-error'">●</span>
                  <span class="font-mono truncate" :title="pod.name">{{ pod.name }}</span>
                </div>
                <div class="flex items-center gap-3 flex-shrink-0 ml-2">
                  <span class="text-text-meta">{{ pod.phase }}</span>
                  <span v-if="pod.restarts > 0" class="text-status-warning">
                    {{ pod.restarts }} restart{{ pod.restarts > 1 ? 's' : '' }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Legend -->
    <div class="mt-8 bg-bg-card rounded-xl border border-border-default p-4">
      <h3 class="text-sm font-semibold text-text-heading mb-3">Status Legend</h3>
      <div class="grid grid-cols-2 md:grid-cols-4 gap-3">
        <div
          v-for="(config, status) in statusConfig"
          :key="status"
          class="flex items-center gap-2"
        >
          <span class="text-lg">{{ config.icon }}</span>
          <span class="text-sm text-text-body">{{ config.label }}</span>
        </div>
      </div>
    </div>

    <!-- Info Note -->
    <div class="mt-4 p-4 bg-bg-elevated rounded-lg border border-border-default">
      <p class="text-sm text-text-meta">
        <strong class="text-text-body">Note:</strong> Services are checked every 10 seconds when auto-refresh is enabled.
        <template v-if="hasK8sInfo"> Replica and pod information is available in Kubernetes environments.</template>
      </p>
    </div>
  </div>
</template>
