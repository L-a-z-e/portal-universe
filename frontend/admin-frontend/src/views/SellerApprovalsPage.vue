<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { Badge, Spinner, Alert, useApiError } from '@portal/design-vue';
import { fetchPendingSellerApplications, reviewSellerApplication } from '@/api/admin';
import type { SellerApplication, PageResponse } from '@/dto/admin';

const { getErrorMessage, handleError } = useApiError();

const data = ref<PageResponse<SellerApplication> | null>(null);
const loading = ref(true);
const error = ref('');

// Filter/Sort
const statusFilter = ref('ALL');
const sortBy = ref('newest');

// Expandable rows
const expandedId = ref<number | null>(null);
const reviewComment = ref('');
const reviewLoading = ref(false);

const filteredItems = computed(() => {
  if (!data.value) return [];
  let items = [...data.value.items];

  if (statusFilter.value !== 'ALL') {
    items = items.filter((i) => i.status === statusFilter.value);
  }

  if (sortBy.value === 'oldest') {
    items.sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime());
  } else {
    items.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
  }

  return items;
});

function statusVariant(status: string): 'warning' | 'success' | 'danger' | 'default' {
  switch (status) {
    case 'PENDING': return 'warning';
    case 'APPROVED': return 'success';
    case 'REJECTED': return 'danger';
    default: return 'default';
  }
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  });
}

function toggleExpand(id: number) {
  if (expandedId.value === id) {
    expandedId.value = null;
  } else {
    expandedId.value = id;
    reviewComment.value = '';
  }
}

async function load() {
  loading.value = true;
  error.value = '';
  try {
    data.value = await fetchPendingSellerApplications();
  } catch (err) {
    console.error('[Admin] Failed to fetch seller applications:', err);
    error.value = getErrorMessage(err, 'Failed to load seller applications.');
  } finally {
    loading.value = false;
  }
}

async function handleReview(id: number, approved: boolean) {
  const comment = approved ? (reviewComment.value || 'Approved') : reviewComment.value;
  if (!approved && !comment) {
    error.value = 'Please provide a rejection reason.';
    return;
  }
  reviewLoading.value = true;
  try {
    await reviewSellerApplication(id, approved, comment);
    expandedId.value = null;
    reviewComment.value = '';
    await load();
  } catch (err) {
    handleError(err, 'Failed to process review.');
  } finally {
    reviewLoading.value = false;
  }
}

onMounted(load);
</script>

<template>
  <div>
    <h1 class="text-2xl font-bold text-text-heading mb-6">Seller Approvals</h1>

    <!-- Error -->
    <Alert v-if="error" variant="error" dismissible class="mb-4" @dismiss="error = ''">
      {{ error }}
    </Alert>

    <!-- Filter Bar -->
    <div class="admin-filter-bar">
      <div class="flex items-center gap-2">
        <span class="text-xs text-text-meta">Status:</span>
        <select
          v-model="statusFilter"
          class="bg-bg-elevated border border-border-default rounded px-3 py-2 text-sm text-text-body"
        >
          <option value="ALL">All</option>
          <option value="PENDING">Pending</option>
          <option value="APPROVED">Approved</option>
          <option value="REJECTED">Rejected</option>
        </select>
      </div>
      <div class="flex items-center gap-2">
        <span class="text-xs text-text-meta">Sort:</span>
        <select
          v-model="sortBy"
          class="bg-bg-elevated border border-border-default rounded px-3 py-2 text-sm text-text-body"
        >
          <option value="newest">Newest first</option>
          <option value="oldest">Oldest first</option>
        </select>
      </div>
      <div class="flex-1"></div>
      <span class="text-xs text-text-meta">
        {{ filteredItems.length }} application{{ filteredItems.length !== 1 ? 's' : '' }}
      </span>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="flex justify-center py-12">
      <Spinner size="lg" />
    </div>

    <!-- Table -->
    <div v-else class="bg-bg-card border border-border-default rounded-lg overflow-hidden">
      <table class="admin-table">
        <thead>
          <tr>
            <th>Business Name</th>
            <th>Business Number</th>
            <th>Applied</th>
            <th>Status</th>
            <th class="w-12"></th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="filteredItems.length === 0">
            <td colspan="5" class="text-center text-text-meta py-8">No applications found</td>
          </tr>
          <template v-for="app in filteredItems" :key="app.id">
            <tr class="cursor-pointer" @click="toggleExpand(app.id)">
              <td class="font-medium">{{ app.businessName }}</td>
              <td class="font-mono text-xs">{{ app.businessNumber }}</td>
              <td class="text-xs text-text-meta">{{ formatDate(app.createdAt) }}</td>
              <td>
                <Badge :variant="statusVariant(app.status)" size="sm">{{ app.status }}</Badge>
              </td>
              <td>
                <span
                  class="material-symbols-outlined text-text-muted transition-transform"
                  :class="{ 'rotate-180': expandedId === app.id }"
                  style="font-size: 18px;"
                >
                  expand_more
                </span>
              </td>
            </tr>
            <!-- Expanded Detail -->
            <tr v-if="expandedId === app.id" class="admin-table-expandable">
              <td colspan="5">
                <div class="max-w-2xl space-y-4">
                  <!-- Applicant Details -->
                  <div class="grid grid-cols-2 gap-4 text-sm">
                    <div>
                      <span class="text-text-meta text-xs">Applicant User ID</span>
                      <p class="text-text-body font-mono text-xs">{{ app.userId }}</p>
                    </div>
                    <div>
                      <span class="text-text-meta text-xs">Business Number</span>
                      <p class="text-text-body font-mono">{{ app.businessNumber }}</p>
                    </div>
                  </div>

                  <!-- Application Reason -->
                  <div>
                    <span class="text-text-meta text-xs">Application Reason</span>
                    <p class="text-sm text-text-body mt-1 p-3 bg-bg-elevated rounded">
                      {{ app.reason || 'No reason provided' }}
                    </p>
                  </div>

                  <!-- Previous Review Info -->
                  <div v-if="app.reviewedBy" class="p-3 bg-bg-elevated rounded">
                    <span class="text-text-meta text-xs">Previous Review</span>
                    <p class="text-sm text-text-body mt-1">
                      Reviewed by {{ app.reviewedBy.slice(0, 8) }}... on {{ app.reviewedAt ? formatDate(app.reviewedAt) : '-' }}
                    </p>
                    <p v-if="app.reviewComment" class="text-sm text-text-meta mt-1">
                      "{{ app.reviewComment }}"
                    </p>
                  </div>

                  <!-- Review Actions -->
                  <div v-if="app.status === 'PENDING'">
                    <label class="text-xs text-text-meta block mb-1">Review Comments</label>
                    <textarea
                      v-model="reviewComment"
                      rows="3"
                      placeholder="Add review comments (required for rejection)..."
                      class="w-full px-3 py-2 bg-bg-elevated border border-border-default rounded text-sm text-text-body placeholder:text-text-muted focus:outline-none focus:border-border-focus resize-none"
                    ></textarea>
                    <div class="flex gap-2 mt-3">
                      <button
                        :disabled="reviewLoading"
                        @click.stop="handleReview(app.id, true)"
                        class="flex items-center gap-1.5 px-4 py-2 bg-status-success text-white rounded text-sm font-medium hover:opacity-90 disabled:opacity-40 transition-colors"
                      >
                        <span class="material-symbols-outlined" style="font-size: 16px;">check</span>
                        Approve
                      </button>
                      <button
                        :disabled="reviewLoading"
                        @click.stop="handleReview(app.id, false)"
                        class="flex items-center gap-1.5 px-4 py-2 bg-status-error text-white rounded text-sm font-medium hover:opacity-90 disabled:opacity-40 transition-colors"
                      >
                        <span class="material-symbols-outlined" style="font-size: 16px;">close</span>
                        Reject
                      </button>
                    </div>
                  </div>
                </div>
              </td>
            </tr>
          </template>
        </tbody>
      </table>
    </div>
  </div>
</template>
