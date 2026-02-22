<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { Badge, Spinner, Alert, Card, Select, Button, Textarea, useApiError, useToast } from '@portal/design-vue';
import { fetchPendingSellerApplications, reviewSellerApplication } from '@/api/admin';
import type { SellerApplication, PageResponse } from '@/dto/admin';

const { getErrorMessage, handleError } = useApiError();
const toast = useToast();

// === State ===
const data = ref<PageResponse<SellerApplication> | null>(null);
const loading = ref(true);
const error = ref('');

const statusFilter = ref<string | number | null>(null);
const sortBy = ref<string | number | null>('newest');

const expandedId = ref<number | null>(null);
const reviewComment = ref('');
const reviewLoading = ref(false);

// === Options ===
const statusOptions = [
  { value: 'PENDING', label: 'Pending' },
  { value: 'APPROVED', label: 'Approved' },
  { value: 'REJECTED', label: 'Rejected' },
];

const sortOptions = [
  { value: 'newest', label: 'Date Applied (Newest)' },
  { value: 'oldest', label: 'Date Applied (Oldest)' },
];

// === Computed ===
const pendingCount = computed(() => {
  if (!data.value) return 0;
  return data.value.items.filter((i) => i.status === 'PENDING').length;
});

const filteredItems = computed(() => {
  if (!data.value) return [];
  let items = [...data.value.items];

  if (statusFilter.value) {
    items = items.filter((i) => i.status === String(statusFilter.value));
  }

  const dir = sortBy.value === 'oldest' ? 1 : -1;
  items.sort((a, b) => dir * (new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()));

  return items;
});

// === Helpers ===
function getInitials(name: string): string {
  return name
    .split(/\s+/)
    .map((w) => w[0])
    .join('')
    .toUpperCase()
    .slice(0, 2);
}

function relativeTime(dateStr: string): string {
  const diff = Date.now() - new Date(dateStr).getTime();
  const minutes = Math.floor(diff / 60000);
  if (minutes < 60) return `${Math.max(1, minutes)}m ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  if (days < 30) return `${days}d ago`;
  return `${Math.floor(days / 30)}mo ago`;
}

function statusVariant(status: string): 'warning' | 'success' | 'danger' | 'default' {
  switch (status) {
    case 'PENDING':
      return 'warning';
    case 'APPROVED':
      return 'success';
    case 'REJECTED':
      return 'danger';
    default:
      return 'default';
  }
}

function toggleExpand(id: number) {
  if (expandedId.value === id) {
    expandedId.value = null;
  } else {
    expandedId.value = id;
    reviewComment.value = '';
  }
}

// === Actions ===
async function load() {
  loading.value = true;
  error.value = '';
  try {
    data.value = await fetchPendingSellerApplications();
  } catch (err) {
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
    toast.success(approved ? 'Application approved.' : 'Application rejected.');
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
    <!-- Header -->
    <div class="flex justify-between items-end mb-6">
      <div>
        <div class="flex items-center gap-3">
          <h1 class="text-2xl font-bold text-text-heading tracking-tight">Seller Approvals</h1>
          <span
            v-if="pendingCount > 0"
            class="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-bg-elevated text-text-heading border border-border-default"
          >
            {{ pendingCount }} Pending
          </span>
        </div>
        <p class="text-text-meta mt-1 text-sm">Review and manage incoming seller requests.</p>
      </div>
    </div>

    <!-- Error -->
    <Alert v-if="error" variant="error" dismissible class="mb-4" @dismiss="error = ''">
      {{ error }}
    </Alert>

    <!-- Filter Bar -->
    <div class="flex flex-wrap gap-4 items-end mb-6">
      <div class="w-[200px]">
        <label class="block text-xs font-semibold text-text-meta mb-1.5 uppercase tracking-wide">Status</label>
        <Select
          v-model="statusFilter"
          :options="statusOptions"
          placeholder="All Statuses"
          clearable
          size="md"
        />
      </div>
      <div class="w-[240px]">
        <label class="block text-xs font-semibold text-text-meta mb-1.5 uppercase tracking-wide">Sort by</label>
        <Select v-model="sortBy" :options="sortOptions" size="md" />
      </div>
      <div class="flex-1" />
      <span class="text-xs text-text-meta font-medium pb-2.5">
        {{ filteredItems.length }} application{{ filteredItems.length !== 1 ? 's' : '' }}
      </span>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="flex justify-center py-12">
      <Spinner size="lg" />
    </div>

    <!-- Table -->
    <Card v-else variant="elevated" padding="none" class="overflow-hidden">
      <table class="admin-table">
        <thead>
          <tr>
            <th>Business Name</th>
            <th>Business Number</th>
            <th>Applied</th>
            <th>Status</th>
            <th class="!text-right">Action</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="filteredItems.length === 0">
            <td colspan="5">
              <div class="flex flex-col items-center justify-center py-16 text-text-muted">
                <span class="material-symbols-outlined mb-3" style="font-size: 48px; opacity: 0.3">
                  verified_user
                </span>
                <p class="text-sm">No applications found</p>
              </div>
            </td>
          </tr>
          <template v-for="app in filteredItems" :key="app.id">
            <!-- Summary Row -->
            <tr
              class="cursor-pointer"
              :class="{ selected: expandedId === app.id }"
              @click="toggleExpand(app.id)"
            >
              <td>
                <div class="flex items-center gap-3">
                  <div
                    class="w-8 h-8 rounded bg-bg-elevated flex items-center justify-center text-xs font-bold text-text-meta shrink-0"
                  >
                    {{ getInitials(app.businessName) }}
                  </div>
                  <span class="font-semibold text-text-heading">{{ app.businessName }}</span>
                </div>
              </td>
              <td class="font-mono text-xs text-text-body">{{ app.businessNumber }}</td>
              <td class="text-sm text-text-meta">{{ relativeTime(app.createdAt) }}</td>
              <td>
                <Badge :variant="statusVariant(app.status)" size="sm">{{ app.status }}</Badge>
              </td>
              <td class="!text-right">
                <span
                  class="material-symbols-outlined text-text-muted transition-transform inline-block"
                  :class="{ 'rotate-180': expandedId === app.id }"
                  style="font-size: 20px"
                >
                  expand_more
                </span>
              </td>
            </tr>

            <!-- Expanded Detail -->
            <tr v-if="expandedId === app.id" class="admin-table-expandable">
              <td colspan="5">
                <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
                  <!-- Left Column: Details (2/3) -->
                  <div class="lg:col-span-2 flex flex-col gap-6">
                    <!-- Applicant Details -->
                    <div>
                      <label class="block text-[10px] uppercase tracking-[0.15em] font-semibold text-text-meta mb-2">
                        Applicant Details
                      </label>
                      <div class="flex items-center gap-3">
                        <div class="w-10 h-10 rounded-full bg-bg-elevated flex items-center justify-center">
                          <span class="material-symbols-outlined text-text-muted" style="font-size: 20px">
                            person
                          </span>
                        </div>
                        <div class="flex-1 min-w-0">
                          <p class="text-sm font-medium text-text-heading">{{ app.businessName }}</p>
                          <p class="text-xs text-text-meta font-mono truncate">{{ app.userId }}</p>
                        </div>
                      </div>
                    </div>

                    <!-- Application Reason -->
                    <div>
                      <label class="block text-[10px] uppercase tracking-[0.15em] font-semibold text-text-meta mb-2">
                        Application Reason
                      </label>
                      <div
                        class="p-4 bg-bg-muted rounded border border-border-muted text-sm text-text-body leading-relaxed"
                      >
                        "{{ app.reason || 'No reason provided' }}"
                      </div>
                    </div>

                    <!-- On Approval Logic -->
                    <div
                      class="p-4 rounded border border-border-default"
                      style="
                        background-color: color-mix(in srgb, var(--semantic-brand-primary) 5%, transparent);
                      "
                    >
                      <div class="flex items-center gap-2 mb-3">
                        <span class="material-symbols-outlined text-brand-primary" style="font-size: 20px">
                          smart_toy
                        </span>
                        <span class="text-sm font-bold text-text-heading uppercase tracking-wide">
                          On Approval Logic
                        </span>
                      </div>
                      <div class="grid grid-cols-2 gap-4">
                        <div class="flex flex-col gap-1">
                          <span class="text-xs text-text-meta">Assigned Role</span>
                          <span class="text-sm font-medium text-text-heading flex items-center gap-1.5">
                            <span class="material-symbols-outlined text-status-success" style="font-size: 16px">
                              check_circle
                            </span>
                            Seller (Level 1)
                          </span>
                        </div>
                        <div class="flex flex-col gap-1">
                          <span class="text-xs text-text-meta">Membership Plan</span>
                          <span class="text-sm font-medium text-text-heading flex items-center gap-1.5">
                            <span class="material-symbols-outlined text-status-success" style="font-size: 16px">
                              check_circle
                            </span>
                            Standard Business
                          </span>
                        </div>
                      </div>
                    </div>

                    <!-- Previous Review (if exists) -->
                    <div v-if="app.reviewedBy" class="p-3 bg-bg-elevated rounded border border-border-muted">
                      <label class="block text-[10px] uppercase tracking-[0.15em] font-semibold text-text-meta mb-2">
                        Previous Review
                      </label>
                      <p class="text-sm text-text-body">
                        Reviewed by
                        <span class="font-mono">{{ app.reviewedBy.slice(0, 8) }}...</span>
                        on {{ app.reviewedAt ? new Date(app.reviewedAt).toLocaleDateString('ko-KR') : '-' }}
                      </p>
                      <p v-if="app.reviewComment" class="text-sm text-text-meta mt-1 italic">
                        "{{ app.reviewComment }}"
                      </p>
                    </div>
                  </div>

                  <!-- Right Column: Review Actions (1/3) -->
                  <div
                    class="lg:col-span-1 flex flex-col justify-between lg:border-l lg:border-border-default lg:pl-8"
                  >
                    <template v-if="app.status === 'PENDING'">
                      <div class="flex flex-col gap-4">
                        <div>
                          <label
                            class="block text-[10px] uppercase tracking-[0.15em] font-semibold text-text-meta mb-2"
                          >
                            Review Comments
                          </label>
                          <Textarea
                            v-model="reviewComment"
                            :rows="5"
                            placeholder="Add an internal note or reason for rejection..."
                            resize="none"
                            @click.stop
                          />
                        </div>
                      </div>
                      <div class="flex flex-col gap-3 mt-6">
                        <Button
                          variant="primary"
                          :disabled="reviewLoading"
                          class="w-full justify-center"
                          @click.stop="handleReview(app.id, true)"
                        >
                          <span class="material-symbols-outlined" style="font-size: 18px">check</span>
                          Approve Application
                        </Button>
                        <Button
                          variant="outline"
                          :disabled="reviewLoading"
                          class="w-full justify-center !text-status-error !border-status-error hover:!bg-status-error/5"
                          @click.stop="handleReview(app.id, false)"
                        >
                          <span class="material-symbols-outlined" style="font-size: 18px">block</span>
                          Reject Application
                        </Button>
                      </div>
                    </template>
                    <div v-else class="flex flex-col items-center justify-center h-full text-text-muted py-8">
                      <span class="material-symbols-outlined mb-2" style="font-size: 32px; opacity: 0.3">
                        task_alt
                      </span>
                      <p class="text-sm">Already {{ app.status.toLowerCase() }}</p>
                    </div>
                  </div>
                </div>
              </td>
            </tr>
          </template>
        </tbody>
      </table>
    </Card>
  </div>
</template>
