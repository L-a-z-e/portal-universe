<script setup lang="ts">
import { ref, onMounted, watch } from 'vue';
import { Spinner, Alert, useApiError } from '@portal/design-vue';
import { fetchAuditLogs } from '@/api/admin';
import type { AuditLog, PageResponse } from '@/dto/admin';

const { getErrorMessage } = useApiError();

const searchActor = ref('');
const eventTypeFilter = ref('');
const page = ref(1);

const data = ref<PageResponse<AuditLog> | null>(null);
const loading = ref(true);
const error = ref('');

// Collect unique event types for dropdown
const eventTypes = ref<string[]>([]);

function formatTimestamp(isoStr: string): string {
  const d = new Date(isoStr);
  return d.toLocaleString('ko-KR', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  });
}

function formatEventType(type: string): string {
  return type.replace(/_/g, ' ');
}

function eventBadgeClass(type: string): string {
  const lower = type.toLowerCase();
  if (lower.includes('assign')) return 'event-badge--assigned';
  if (lower.includes('revoke') || lower.includes('remove')) return 'event-badge--revoked';
  if (lower.includes('create') || lower.includes('register')) return 'event-badge--created';
  if (lower.includes('fail') || lower.includes('error')) return 'event-badge--failed';
  if (lower.includes('config') || lower.includes('update') || lower.includes('change')) return 'event-badge--config';
  return 'event-badge--default';
}

function truncateId(id: string | null): string {
  if (!id) return '-';
  return id.length > 8 ? id.slice(0, 8) + '...' : id;
}

async function load() {
  loading.value = true;
  error.value = '';
  try {
    data.value = await fetchAuditLogs(page.value, 20, searchActor.value || undefined);
    // Collect unique event types
    if (data.value?.items) {
      const types = new Set(eventTypes.value);
      data.value.items.forEach((item) => types.add(item.eventType));
      eventTypes.value = Array.from(types).sort();
    }
  } catch (err) {
    console.error('[Admin] Failed to fetch audit logs:', err);
    error.value = getErrorMessage(err, 'Failed to load audit logs.');
  } finally {
    loading.value = false;
  }
}

function handleFilter() {
  page.value = 1;
  load();
}

function clearFilter() {
  searchActor.value = '';
  eventTypeFilter.value = '';
  page.value = 1;
  load();
}

function exportCsv() {
  if (!data.value?.items.length) return;
  const headers = ['Timestamp', 'Event Type', 'Actor', 'Target', 'Details', 'IP'];
  const rows = data.value.items.map((log) => [
    log.createdAt,
    log.eventType,
    log.actorUserId,
    log.targetUserId,
    `"${log.details.replace(/"/g, '""')}"`,
    log.ipAddress ?? '',
  ]);
  const csv = [headers.join(','), ...rows.map((r) => r.join(','))].join('\n');
  const blob = new Blob([csv], { type: 'text/csv' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `audit-log-${new Date().toISOString().slice(0, 10)}.csv`;
  a.click();
  URL.revokeObjectURL(url);
}

// Filter by event type (client-side for current page)
const filteredItems = ref<AuditLog[]>([]);
watch([data, eventTypeFilter], () => {
  if (!data.value?.items) {
    filteredItems.value = [];
    return;
  }
  if (!eventTypeFilter.value) {
    filteredItems.value = data.value.items;
    return;
  }
  filteredItems.value = data.value.items.filter((i) => i.eventType === eventTypeFilter.value);
}, { immediate: true });

watch(page, load);
onMounted(load);
</script>

<template>
  <div>
    <div class="flex items-center justify-between mb-6">
      <h1 class="text-2xl font-bold text-text-heading">Audit Log</h1>
      <button
        @click="exportCsv"
        :disabled="!data?.items?.length"
        class="flex items-center gap-1.5 px-3 py-2 border border-border-default rounded-lg text-sm text-text-body hover:bg-bg-hover disabled:opacity-40 transition-colors"
      >
        <span class="material-symbols-outlined" style="font-size: 16px;">download</span>
        Export CSV
      </button>
    </div>

    <!-- Error -->
    <Alert v-if="error" variant="error" dismissible class="mb-4" @dismiss="error = ''">
      {{ error }}
    </Alert>

    <!-- Filter Bar -->
    <div class="admin-filter-bar">
      <div class="relative flex-1 max-w-xs">
        <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-text-muted" style="font-size: 18px;">search</span>
        <input
          v-model="searchActor"
          type="text"
          placeholder="Search by Actor UUID..."
          class="w-full pl-9 pr-3 py-2 bg-bg-elevated border border-border-default rounded text-sm text-text-body placeholder:text-text-muted focus:outline-none focus:border-border-focus"
          @keyup.enter="handleFilter"
        />
      </div>
      <select
        v-model="eventTypeFilter"
        class="bg-bg-elevated border border-border-default rounded px-3 py-2 text-sm text-text-body"
      >
        <option value="">All Events</option>
        <option v-for="et in eventTypes" :key="et" :value="et">
          {{ formatEventType(et) }}
        </option>
      </select>
      <button
        @click="handleFilter"
        class="px-4 py-2 bg-brand-primary text-white rounded text-sm font-medium hover:bg-brand-primaryHover transition-colors"
      >
        Filter
      </button>
      <button
        v-if="searchActor || eventTypeFilter"
        @click="clearFilter"
        class="text-xs text-text-meta hover:text-text-body"
      >
        Clear
      </button>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="flex justify-center py-12">
      <Spinner size="lg" />
    </div>

    <!-- Log Table -->
    <div v-else class="bg-bg-card border border-border-default rounded-lg overflow-hidden">
      <table class="admin-table">
        <thead>
          <tr>
            <th class="w-36">Timestamp</th>
            <th>Event Detail</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="filteredItems.length === 0">
            <td colspan="2" class="text-center text-text-meta py-8">No audit logs found</td>
          </tr>
          <tr v-for="log in filteredItems" :key="log.id">
            <td class="text-xs text-text-meta whitespace-nowrap align-top">
              {{ formatTimestamp(log.createdAt) }}
            </td>
            <td>
              <div class="flex items-start gap-2">
                <span :class="['event-badge', eventBadgeClass(log.eventType)]">
                  {{ formatEventType(log.eventType) }}
                </span>
                <div class="text-sm">
                  <span class="font-mono text-xs text-text-meta">{{ truncateId(log.actorUserId) }}</span>
                  <span class="text-text-muted mx-1">&rarr;</span>
                  <span class="font-mono text-xs text-text-meta">{{ truncateId(log.targetUserId) }}</span>
                  <p class="text-text-body text-xs mt-0.5">{{ log.details }}</p>
                </div>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Pagination -->
    <div v-if="data && data.totalPages > 1" class="flex items-center justify-between mt-4">
      <span class="text-xs text-text-meta">
        Page {{ page }} of {{ data.totalPages }}
      </span>
      <div class="flex gap-1">
        <button
          @click="page = Math.max(1, page - 1)"
          :disabled="page === 1"
          class="px-3 py-1.5 border border-border-default rounded text-sm text-text-body hover:bg-bg-hover disabled:opacity-30 transition-colors"
        >
          Prev
        </button>
        <button
          @click="page = Math.min(data.totalPages, page + 1)"
          :disabled="page >= data.totalPages"
          class="px-3 py-1.5 border border-border-default rounded text-sm text-text-body hover:bg-bg-hover disabled:opacity-30 transition-colors"
        >
          Next
        </button>
      </div>
    </div>
  </div>
</template>
