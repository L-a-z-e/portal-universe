<script setup lang="ts">
import { ref, onMounted, watch } from 'vue';
import { useApiError } from '@portal/design-vue';
import { fetchAuditLogs } from '@/api/admin';
import type { AuditLog, PageResponse } from '@/dto/admin';

const { getErrorMessage } = useApiError();

const userId = ref('');
const searchedUserId = ref('');
const page = ref(1);

const data = ref<PageResponse<AuditLog> | null>(null);
const loading = ref(true);
const error = ref('');

async function load() {
  loading.value = true;
  error.value = '';
  try {
    data.value = await fetchAuditLogs(page.value, 20, searchedUserId.value || undefined);
  } catch (err) {
    console.error('[Admin] Failed to fetch audit logs:', err);
    error.value = getErrorMessage(err, 'Failed to load audit logs.');
  } finally {
    loading.value = false;
  }
}

function search() {
  searchedUserId.value = userId.value;
  page.value = 1;
  load();
}

function clearFilter() {
  searchedUserId.value = '';
  userId.value = '';
  page.value = 1;
  load();
}

watch(page, load);
onMounted(load);
</script>

<template>
  <div>
    <h1 class="text-2xl font-bold text-text-heading mb-6">Audit Log</h1>

    <div class="flex gap-2 mb-4">
      <input
        v-model="userId"
        type="text"
        placeholder="Filter by User UUID (optional)"
        class="border border-border-default rounded px-3 py-2 text-sm flex-1 max-w-md bg-bg-card text-text-body"
        @keyup.enter="search"
      />
      <button
        @click="search"
        class="bg-brand-primary text-white px-4 py-2 rounded text-sm hover:opacity-90"
      >
        Filter
      </button>
      <button
        v-if="searchedUserId"
        @click="clearFilter"
        class="text-text-meta px-4 py-2 text-sm hover:underline"
      >
        Clear
      </button>
    </div>

    <div v-if="error" class="mb-4 p-3 bg-status-error-bg text-status-error rounded text-sm">
      {{ error }}
    </div>

    <div class="bg-bg-card rounded-lg shadow overflow-hidden border border-border-default">
      <table class="w-full text-sm">
        <thead class="bg-bg-elevated border-b border-border-default">
          <tr>
            <th class="text-left p-3 text-text-heading">Time</th>
            <th class="text-left p-3 text-text-heading">Event</th>
            <th class="text-left p-3 text-text-heading">Actor</th>
            <th class="text-left p-3 text-text-heading">Target</th>
            <th class="text-left p-3 text-text-heading">Details</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td colspan="5" class="p-4 text-center text-text-meta">Loading...</td>
          </tr>
          <tr
            v-for="log in data?.items"
            :key="log.id"
            class="border-b border-border-default hover:bg-bg-elevated transition-colors"
          >
            <td class="p-3 text-xs text-text-meta whitespace-nowrap">{{ new Date(log.createdAt).toLocaleString() }}</td>
            <td class="p-3">
              <span class="bg-bg-elevated px-2 py-0.5 rounded text-xs font-mono text-text-body">{{ log.eventType }}</span>
            </td>
            <td class="p-3 font-mono text-xs text-text-body">{{ log.actorUserId?.slice(0, 8) }}...</td>
            <td class="p-3 font-mono text-xs text-text-body">{{ log.targetUserId?.slice(0, 8) }}...</td>
            <td class="p-3 text-text-meta text-xs max-w-md truncate">{{ log.details }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="data && data.totalPages > 1" class="flex justify-center gap-2 mt-4">
      <button
        @click="page = Math.max(1, page - 1)"
        :disabled="page === 1"
        class="px-3 py-1 border border-border-default rounded text-sm text-text-body disabled:opacity-30"
      >
        Prev
      </button>
      <span class="px-3 py-1 text-sm text-text-meta">
        {{ page }} / {{ data.totalPages }}
      </span>
      <button
        @click="page = Math.min(data.totalPages, page + 1)"
        :disabled="page >= data.totalPages"
        class="px-3 py-1 border border-border-default rounded text-sm text-text-body disabled:opacity-30"
      >
        Next
      </button>
    </div>
  </div>
</template>
