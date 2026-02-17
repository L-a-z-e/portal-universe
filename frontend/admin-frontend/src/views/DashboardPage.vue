<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { Card, Button, Badge, Spinner, Alert, useApiError } from '@portal/design-vue';
import { fetchDashboardStats } from '@/api/admin';
import type { DashboardStats } from '@/dto/admin';

const router = useRouter();
const { getErrorMessage } = useApiError();
const stats = ref<DashboardStats | null>(null);
const loading = ref(true);
const error = ref('');

onMounted(async () => {
  try {
    stats.value = await fetchDashboardStats();
  } catch (err) {
    console.error('[Admin] Failed to fetch dashboard stats:', err);
    error.value = getErrorMessage(err, 'Failed to load dashboard data.');
  } finally {
    loading.value = false;
  }
});

function timeAgo(isoStr: string): string {
  const diff = Date.now() - new Date(isoStr).getTime();
  const mins = Math.floor(diff / 60_000);
  if (mins < 1) return 'just now';
  if (mins < 60) return `${mins}m ago`;
  const hours = Math.floor(mins / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  return `${days}d ago`;
}

function formatEventType(type: string): string {
  return type.replace(/_/g, ' ');
}

function truncateId(id: string | null): string {
  if (!id) return '-';
  return id.length > 8 ? id.slice(0, 8) + '...' : id;
}

const quickLinks = [
  { label: 'Users', route: 'Users' },
  { label: 'Roles', route: 'Roles' },
  { label: 'Memberships', route: 'Memberships' },
  { label: 'Seller Approvals', route: 'SellerApprovals' },
  { label: 'Audit Log', route: 'AuditLog' },
];
</script>

<template>
  <div>
    <h1 class="text-2xl font-bold text-text-heading mb-6">Dashboard</h1>

    <!-- Loading state -->
    <div v-if="loading" class="flex justify-center py-12">
      <Spinner size="lg" label="Loading dashboard..." />
    </div>

    <!-- Error state -->
    <Alert v-else-if="error" variant="error" :title="error">
      <template #action>
        <Button variant="ghost" size="sm" @click="$router.go(0)">Retry</Button>
      </template>
    </Alert>

    <!-- Dashboard content -->
    <template v-else-if="stats">
      <!-- Section 1: Overview KPI Cards -->
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        <Card variant="outlined" padding="lg">
          <div class="text-sm text-text-meta mb-1">Total Users</div>
          <div class="text-3xl font-bold text-text-heading">{{ stats.users.total }}</div>
        </Card>
        <Card variant="outlined" padding="lg">
          <div class="text-sm text-text-meta mb-1">Role Assignments</div>
          <div class="text-3xl font-bold text-text-heading">
            {{ stats.roles.assignments.reduce((sum, r) => sum + r.userCount, 0) }}
          </div>
        </Card>
        <Card variant="outlined" padding="lg">
          <div class="text-sm text-text-meta mb-1">Active Memberships</div>
          <div class="text-3xl font-bold text-text-heading">
            {{ stats.memberships.groups.reduce((sum, g) => sum + g.activeCount, 0) }}
          </div>
        </Card>
        <Card variant="outlined" padding="lg">
          <div class="text-sm text-text-meta mb-1">Pending Approvals</div>
          <div class="text-3xl font-bold text-brand-primary">{{ stats.sellers.pending }}</div>
        </Card>
      </div>

      <!-- Section 2 & 3: Role Distribution + Membership Overview -->
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-4 mb-6">
        <!-- Role Distribution -->
        <Card variant="outlined" padding="lg">
          <h2 class="text-lg font-semibold text-text-heading mb-4">Role Distribution</h2>
          <div class="space-y-2">
            <div
              v-for="role in stats.roles.assignments"
              :key="role.roleKey"
              class="flex items-center justify-between"
            >
              <div class="flex items-center gap-2">
                <span class="w-2 h-2 rounded-full bg-brand-primary inline-block"></span>
                <span class="text-sm text-text-body font-mono">{{ role.roleKey }}</span>
              </div>
              <span class="text-sm font-semibold text-text-heading">{{ role.userCount }}</span>
            </div>
          </div>
        </Card>

        <!-- Membership Overview -->
        <Card variant="outlined" padding="lg">
          <h2 class="text-lg font-semibold text-text-heading mb-4">Membership Overview</h2>
          <div class="space-y-4">
            <div v-for="group in stats.memberships.groups" :key="group.group">
              <div class="flex items-center justify-between mb-1">
                <span class="text-sm font-medium text-text-body">{{ group.group }}</span>
                <span class="text-xs text-text-meta">{{ group.activeCount }} active</span>
              </div>
              <div class="flex flex-wrap gap-2">
                <Badge
                  v-for="tier in group.tiers"
                  :key="tier.tierKey"
                  variant="default"
                  size="sm"
                >
                  {{ tier.displayName }}: <strong>{{ tier.count }}</strong>
                </Badge>
                <span
                  v-if="group.tiers.length === 0"
                  class="text-xs text-text-meta"
                >No tiers configured</span>
              </div>
            </div>
          </div>
        </Card>
      </div>

      <!-- Section 4: Recent Activity -->
      <Card variant="outlined" padding="none" class="mb-6">
        <div class="flex items-center justify-between p-5 pb-3">
          <h2 class="text-lg font-semibold text-text-heading">Recent Activity</h2>
          <Button
            variant="ghost"
            size="sm"
            @click="router.push({ name: 'AuditLog' })"
          >
            View all
          </Button>
        </div>
        <div class="overflow-x-auto">
          <table class="w-full text-sm">
            <thead class="bg-bg-elevated border-y border-border-default">
              <tr>
                <th class="text-left p-3 text-text-heading">Event</th>
                <th class="text-left p-3 text-text-heading">Target</th>
                <th class="text-left p-3 text-text-heading">Details</th>
                <th class="text-left p-3 text-text-heading">Time</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="stats.recentActivity.length === 0">
                <td colspan="4" class="p-4 text-center text-text-meta">No recent activity</td>
              </tr>
              <tr
                v-for="(activity, idx) in stats.recentActivity"
                :key="idx"
                class="border-b border-border-default last:border-b-0"
              >
                <td class="p-3">
                  <Badge variant="default" size="sm">
                    {{ formatEventType(activity.eventType) }}
                  </Badge>
                </td>
                <td class="p-3 font-mono text-xs text-text-body">{{ truncateId(activity.targetUserId) }}</td>
                <td class="p-3 text-xs text-text-meta max-w-xs truncate">{{ activity.details }}</td>
                <td class="p-3 text-xs text-text-meta whitespace-nowrap">
                  {{ activity.createdAt ? timeAgo(activity.createdAt) : '-' }}
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </Card>

      <!-- Section 5: Quick Links -->
      <Card variant="outlined" padding="lg">
        <h2 class="text-lg font-semibold text-text-heading mb-3">Quick Links</h2>
        <div class="flex flex-wrap gap-2">
          <Button
            v-for="link in quickLinks"
            :key="link.route"
            variant="outline"
            size="sm"
            @click="router.push({ name: link.route })"
          >
            {{ link.label }}
          </Button>
        </div>
      </Card>
    </template>
  </div>
</template>
