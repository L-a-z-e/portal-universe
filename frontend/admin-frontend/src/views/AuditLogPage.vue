<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { Spinner, Alert, Select, Button, Card, Pagination, useApiError, useToast } from '@portal/design-vue';
import { fetchAuditLogs } from '@/api/admin';
import type { AuditLog, PageResponse } from '@/dto/admin';

const { getErrorMessage } = useApiError();
const toast = useToast();

// === State ===
const searchQuery = ref('');
const eventTypeFilter = ref<string | number | null>(null);
const page = ref(1);
const pageSize = 20;

const data = ref<PageResponse<AuditLog> | null>(null);
const loading = ref(true);
const error = ref('');

// Collect unique event types for dropdown
const eventTypes = ref<string[]>([]);

const eventTypeOptions = computed(() =>
  eventTypes.value.map((et) => ({ value: et, label: et })),
);

// Client-side event type filter
const filteredItems = computed(() => {
  if (!data.value?.items) return [];
  if (!eventTypeFilter.value) return data.value.items;
  return data.value.items.filter((i) => i.eventType === String(eventTypeFilter.value));
});

// Pagination
const totalItems = computed(() => data.value?.totalElements ?? 0);
const totalPages = computed(() => data.value?.totalPages ?? 0);
const showingStart = computed(() => totalItems.value === 0 ? 0 : (page.value - 1) * pageSize + 1);
const showingEnd = computed(() => Math.min(page.value * pageSize, totalItems.value));

// === Helpers ===
function formatTimestamp(isoStr: string): string {
  const d = new Date(isoStr);
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  const hours = String(d.getHours()).padStart(2, '0');
  const minutes = String(d.getMinutes()).padStart(2, '0');
  const seconds = String(d.getSeconds()).padStart(2, '0');
  return `${month}-${day} ${hours}:${minutes}:${seconds}`;
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

function actorIcon(actorId: string | null): string {
  if (!actorId) return 'person';
  if (actorId.startsWith('sys') || actorId.includes('bot')) return 'smart_toy';
  if (/^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}$/.test(actorId)) return 'public';
  return 'person';
}

function targetIcon(targetId: string | null): string {
  if (!targetId) return 'target';
  if (targetId === 'system' || targetId.includes('settings')) return 'target';
  return 'target';
}

// === Actions ===
async function load() {
  loading.value = true;
  error.value = '';
  try {
    data.value = await fetchAuditLogs(page.value, pageSize, searchQuery.value || undefined);
    if (data.value?.items) {
      const types = new Set(eventTypes.value);
      data.value.items.forEach((item) => types.add(item.eventType));
      eventTypes.value = Array.from(types).sort();
    }
  } catch (err) {
    error.value = getErrorMessage(err, 'Failed to load audit logs.');
  } finally {
    loading.value = false;
  }
}

function handleFilter() {
  if (page.value === 1) {
    load();
  } else {
    page.value = 1;
  }
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
  toast.success('CSV exported successfully');
}

watch(page, load);
onMounted(load);
</script>

<template>
  <div>
    <!-- Header -->
    <div class="flex justify-between items-end mb-6">
      <div>
        <h1 class="text-2xl font-bold text-text-heading tracking-tight">Audit Logs</h1>
        <p class="text-text-meta mt-1 text-sm">
          Review detailed system events, security alerts, and administrative actions.
        </p>
      </div>
      <Button
        variant="outline"
        size="sm"
        :disabled="!data?.items?.length"
        @click="exportCsv"
      >
        <span class="material-symbols-outlined" style="font-size: 16px;">download</span>
        Export CSV
      </Button>
    </div>

    <!-- Error -->
    <Alert v-if="error" variant="error" dismissible class="mb-4" @dismiss="error = ''">
      {{ error }}
    </Alert>

    <!-- Filter Bar -->
    <Card variant="elevated" padding="md" class="mb-6">
      <div class="flex flex-wrap gap-4 items-end">
        <!-- Search Actor / Target -->
        <div class="flex-1 min-w-[240px]">
          <label class="block text-xs font-semibold text-text-meta mb-1.5 uppercase tracking-wide">
            Search Actor / Target
          </label>
          <div class="relative">
            <span
              class="absolute left-3 top-1/2 -translate-y-1/2 material-symbols-outlined text-text-muted"
              style="font-size: 20px;"
            >search</span>
            <input
              v-model="searchQuery"
              type="text"
              placeholder="UUID or Email address..."
              class="w-full pl-10 pr-4 py-2.5 bg-bg-muted border border-transparent focus:bg-bg-card focus:border-border-focus rounded text-sm text-text-heading font-mono placeholder:text-text-muted placeholder:font-sans focus:outline-none transition-all"
              @keyup.enter="handleFilter"
            />
          </div>
        </div>

        <!-- Event Type -->
        <div class="w-full sm:w-[220px]">
          <label class="block text-xs font-semibold text-text-meta mb-1.5 uppercase tracking-wide">
            Event Type
          </label>
          <Select
            v-model="eventTypeFilter"
            :options="eventTypeOptions"
            placeholder="All Events"
            clearable
            size="md"
          />
        </div>

        <!-- Date Range (backend not ready) -->
        <div class="w-full sm:w-[240px]">
          <label class="block text-xs font-semibold text-text-meta mb-1.5 uppercase tracking-wide">
            Date Range
          </label>
          <div class="relative">
            <span
              class="absolute left-3 top-1/2 -translate-y-1/2 material-symbols-outlined text-text-muted"
              style="font-size: 20px;"
            >calendar_today</span>
            <input
              type="text"
              placeholder="Select dates"
              disabled
              class="w-full pl-10 pr-4 py-2.5 bg-bg-muted border border-transparent rounded text-sm text-text-heading placeholder:text-text-muted focus:outline-none cursor-not-allowed opacity-50 transition-all"
              title="Date range filter — requires backend support"
            />
          </div>
        </div>

        <!-- Filter Action -->
        <Button variant="primary" @click="handleFilter" class="h-[42px]">
          <span class="material-symbols-outlined" style="font-size: 18px;">filter_list</span>
          Filter
        </Button>
      </div>
    </Card>

    <!-- Loading -->
    <div v-if="loading" class="flex justify-center py-12">
      <Spinner size="lg" />
    </div>

    <!-- Log Content -->
    <template v-else>
      <!-- Column Headers -->
      <div class="flex px-4 mb-2 text-xs font-semibold text-text-meta uppercase tracking-wider opacity-60">
        <div class="w-48">Timestamp</div>
        <div class="flex-1">Event Detail</div>
      </div>

      <!-- Log List -->
      <Card variant="elevated" padding="none" class="overflow-hidden">
        <!-- Empty State -->
        <div v-if="filteredItems.length === 0" class="flex flex-col items-center justify-center py-16 text-text-muted">
          <span class="material-symbols-outlined mb-3" style="font-size: 48px; opacity: 0.3;">history_edu</span>
          <p class="text-sm">No audit logs found</p>
        </div>

        <!-- Log Items -->
        <div
          v-for="(log, index) in filteredItems"
          :key="log.id"
          :class="[
            'group hover:bg-bg-hover/40 transition-colors p-4',
            index < filteredItems.length - 1 ? 'border-b border-border-default' : '',
          ]"
        >
          <!-- Line 1: Meta -->
          <div class="flex flex-col sm:flex-row sm:items-baseline gap-2 sm:gap-4 mb-2">
            <!-- Timestamp -->
            <div class="w-48 shrink-0 font-mono text-xs text-text-meta flex items-center gap-2">
              <span class="material-symbols-outlined opacity-60" style="font-size: 16px;">schedule</span>
              {{ formatTimestamp(log.createdAt) }}
            </div>

            <!-- Event Badge & Actor/Target -->
            <div class="flex flex-wrap items-center gap-3 text-sm">
              <span :class="['event-badge', eventBadgeClass(log.eventType)]">
                {{ log.eventType }}
              </span>
              <div class="flex items-center gap-1.5 text-text-meta text-xs">
                <span class="material-symbols-outlined text-text-muted/60" style="font-size: 14px;">
                  {{ actorIcon(log.actorUserId) }}
                </span>
                <span class="font-mono text-text-heading">{{ truncateId(log.actorUserId) }}</span>
                <span class="text-text-muted/50 px-1">&rarr;</span>
                <span class="material-symbols-outlined text-text-muted/60" style="font-size: 14px;">
                  {{ targetIcon(log.targetUserId) }}
                </span>
                <span class="font-mono text-text-heading">{{ truncateId(log.targetUserId) }}</span>
              </div>
            </div>
          </div>

          <!-- Line 2: Details (indented to align with event detail) -->
          <div class="pl-0 sm:pl-[208px]">
            <p class="text-sm text-text-body leading-relaxed">
              {{ log.details }}
            </p>
          </div>
        </div>
      </Card>

      <!-- Pagination -->
      <div v-if="totalPages > 0" class="flex justify-between items-center mt-4">
        <span class="text-xs text-text-meta font-medium">
          Showing {{ showingStart }}-{{ showingEnd }} of {{ totalItems.toLocaleString() }} logs
        </span>
        <Pagination
          v-model="page"
          :page="page"
          :total-pages="totalPages"
          :show-first-last="false"
          size="sm"
        />
      </div>
    </template>
  </div>
</template>
