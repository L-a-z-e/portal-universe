<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { fetchPendingSellerApplications, reviewSellerApplication } from '@/api/admin';
import type { SellerApplication, PageResponse } from '@/dto/admin';

const data = ref<PageResponse<SellerApplication> | null>(null);
const loading = ref(true);
const reviewing = ref(false);

async function load() {
  loading.value = true;
  try {
    data.value = await fetchPendingSellerApplications();
  } catch (err) {
    console.error('[Admin] Failed to fetch seller applications:', err);
  } finally {
    loading.value = false;
  }
}

async function handleReview(id: number, approved: boolean) {
  const comment = approved ? 'Approved' : prompt('Rejection reason:');
  if (comment === null) return;
  reviewing.value = true;
  try {
    await reviewSellerApplication(id, approved, comment || '');
    await load();
  } finally {
    reviewing.value = false;
  }
}

onMounted(load);
</script>

<template>
  <div>
    <h1 class="text-2xl font-bold text-text-heading mb-6">Seller Approvals</h1>
    <div class="bg-bg-card rounded-lg shadow overflow-hidden border border-border-default">
      <table class="w-full text-sm">
        <thead class="bg-bg-elevated border-b border-border-default">
          <tr>
            <th class="text-left p-3 text-text-heading">Business Name</th>
            <th class="text-left p-3 text-text-heading">Business No.</th>
            <th class="text-left p-3 text-text-heading">Reason</th>
            <th class="text-left p-3 text-text-heading">Applied</th>
            <th class="text-left p-3 text-text-heading">Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td colspan="5" class="p-4 text-center text-text-meta">Loading...</td>
          </tr>
          <tr v-else-if="data?.content.length === 0">
            <td colspan="5" class="p-4 text-center text-text-meta">No pending applications</td>
          </tr>
          <tr
            v-for="app in data?.content"
            :key="app.id"
            class="border-b border-border-default hover:bg-bg-elevated transition-colors"
          >
            <td class="p-3 font-medium text-text-body">{{ app.businessName }}</td>
            <td class="p-3 font-mono text-xs text-text-body">{{ app.businessNumber }}</td>
            <td class="p-3 text-text-meta max-w-xs truncate">{{ app.reason }}</td>
            <td class="p-3 text-text-meta text-xs">{{ new Date(app.createdAt).toLocaleDateString() }}</td>
            <td class="p-3 space-x-2">
              <button
                @click="handleReview(app.id, true)"
                :disabled="reviewing"
                class="bg-status-success text-white px-3 py-1 rounded text-xs hover:opacity-90 disabled:opacity-50"
              >
                Approve
              </button>
              <button
                @click="handleReview(app.id, false)"
                :disabled="reviewing"
                class="bg-status-error text-white px-3 py-1 rounded text-xs hover:opacity-90 disabled:opacity-50"
              >
                Reject
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
