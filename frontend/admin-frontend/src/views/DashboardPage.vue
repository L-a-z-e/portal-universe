<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { Spinner, Alert, Button, Card, Badge, Progress, useApiError } from '@portal/design-vue';
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

function eventBadgeClass(type: string): string {
  const lower = type.toLowerCase();
  if (lower.includes('assign')) return 'event-badge--assigned';
  if (lower.includes('revoke') || lower.includes('remove')) return 'event-badge--revoked';
  if (lower.includes('create') || lower.includes('register')) return 'event-badge--created';
  if (lower.includes('fail') || lower.includes('error')) return 'event-badge--failed';
  if (lower.includes('config') || lower.includes('update')) return 'event-badge--config';
  return 'event-badge--default';
}

const totalRoleAssignments = (s: DashboardStats) =>
  s.roles.assignments.reduce((sum, r) => sum + r.userCount, 0);

const totalActiveMemberships = (s: DashboardStats) =>
  s.memberships.groups.reduce((sum, g) => sum + g.activeCount, 0);

function rolePercentage(userCount: number, total: number): number {
  if (total === 0) return 0;
  return Math.round((userCount / total) * 100);
}
</script>

<template>
  <div class="flex flex-col h-[calc(100vh-11rem)] overflow-hidden">
    <h1 class="text-xl font-bold text-text-heading mb-4 shrink-0">Dashboard</h1>

    <!-- Loading -->
    <div v-if="loading" class="flex justify-center py-12">
      <Spinner size="lg" label="Loading dashboard..." />
    </div>

    <!-- Error -->
    <Alert v-else-if="error" variant="error" :title="error">
      <template #action>
        <Button variant="ghost" size="sm" @click="$router.go(0)">Retry</Button>
      </template>
    </Alert>

    <!-- Dashboard content -->
    <template v-else-if="stats">
      <!-- KPI Cards -->
      <div class="grid grid-cols-2 lg:grid-cols-4 gap-3 mb-4 shrink-0">
        <div class="stat-card">
          <div class="stat-icon">
            <span class="material-symbols-outlined" style="font-size: 22px;">group</span>
          </div>
          <div class="stat-label">Total Users</div>
          <div class="stat-value">{{ stats.users.total }}</div>
          <div class="stat-trend stat-trend--up">
            <span class="material-symbols-outlined" style="font-size: 14px;">trending_up</span>
            Active system
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">
            <span class="material-symbols-outlined" style="font-size: 22px;">shield</span>
          </div>
          <div class="stat-label">Role Assignments</div>
          <div class="stat-value">{{ totalRoleAssignments(stats) }}</div>
          <div class="stat-trend">
            <span class="text-text-meta">{{ stats.roles.total }} roles defined</span>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">
            <span class="material-symbols-outlined" style="font-size: 22px;">card_membership</span>
          </div>
          <div class="stat-label">Active Memberships</div>
          <div class="stat-value">{{ totalActiveMemberships(stats) }}</div>
          <div class="stat-trend">
            <span class="text-text-meta">{{ stats.memberships.groups.length }} groups</span>
          </div>
        </div>
        <div class="stat-card" :class="{ 'stat-card--warning': stats.sellers.pending > 0 }">
          <div class="stat-icon">
            <span class="material-symbols-outlined" style="font-size: 22px;">approval</span>
          </div>
          <div class="stat-label">Pending Approvals</div>
          <div class="stat-value" :class="stats.sellers.pending > 0 ? 'text-status-warning' : ''">
            {{ stats.sellers.pending }}
          </div>
          <div v-if="stats.sellers.pending > 0" class="stat-trend">
            <Button variant="ghost" size="xs" @click="router.push({ name: 'SellerApprovals' })">
              Review now
            </Button>
          </div>
        </div>
      </div>

      <!-- Middle: Recent Activity (left) + Role Distribution (right) -->
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-4 mb-4 flex-1 min-h-0 max-h-[45vh] overflow-hidden">
        <!-- Recent Activity -->
        <Card variant="elevated" padding="none" class="flex flex-col overflow-hidden min-h-0">
          <div class="flex items-center justify-between px-5 py-3 shrink-0 border-b border-border-muted">
            <h2 class="text-sm font-semibold text-text-heading">Recent Activity</h2>
            <Button variant="ghost" size="xs" @click="router.push({ name: 'AuditLog' })">
              View all
            </Button>
          </div>
          <div class="flex-1 overflow-y-auto px-5 py-4">
            <div v-if="stats.recentActivity.length === 0" class="text-sm text-text-meta text-center py-8">
              No recent activity
            </div>
            <div v-else class="admin-timeline">
              <div
                v-for="(activity, idx) in stats.recentActivity.slice(0, 8)"
                :key="idx"
                class="admin-timeline-item"
              >
                <div class="timeline-time">{{ activity.createdAt ? timeAgo(activity.createdAt) : '-' }}</div>
                <div class="timeline-content">
                  <span :class="['event-badge', eventBadgeClass(activity.eventType)]">
                    {{ formatEventType(activity.eventType) }}
                  </span>
                  <span class="ml-2">{{ activity.details }}</span>
                </div>
              </div>
            </div>
          </div>
        </Card>

        <!-- Role Distribution -->
        <Card variant="elevated" padding="none" class="flex flex-col overflow-hidden min-h-0">
          <div class="px-5 py-3 shrink-0 border-b border-border-muted">
            <h2 class="text-sm font-semibold text-text-heading">Role Distribution</h2>
          </div>
          <div class="flex-1 overflow-y-auto px-5 py-4">
            <div class="space-y-3">
              <div
                v-for="role in stats.roles.assignments"
                :key="role.roleKey"
              >
                <div class="flex items-center justify-between mb-1">
                  <span class="text-sm text-text-body">{{ role.displayName }}</span>
                  <span class="text-xs text-text-meta font-medium">
                    {{ rolePercentage(role.userCount, totalRoleAssignments(stats)) }}%
                  </span>
                </div>
                <Progress
                  :value="Math.max(rolePercentage(role.userCount, totalRoleAssignments(stats)), 1)"
                  :max="100"
                  size="sm"
                />
              </div>
            </div>
          </div>
        </Card>
      </div>

      <!-- Bottom: Membership Groups -->
      <Card variant="elevated" padding="none" class="flex flex-col overflow-hidden shrink-0 max-h-[280px]">
        <div class="px-5 py-3 border-b border-border-muted shrink-0 flex items-center justify-between">
          <h2 class="text-sm font-semibold text-text-heading">Membership Groups</h2>
          <span class="text-xs text-text-meta">{{ stats.memberships.groups.length }} groups</span>
        </div>
        <div class="flex-1 overflow-y-auto">
          <table class="admin-table">
            <thead class="sticky top-0 z-10">
              <tr>
                <th>Group</th>
                <th>Active Members</th>
                <th>Tiers</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="group in stats.memberships.groups" :key="group.group">
                <td class="font-medium">{{ group.group }}</td>
                <td>{{ group.activeCount }}</td>
                <td>
                  <div class="flex flex-wrap gap-1">
                    <Badge
                      v-for="tier in group.tiers"
                      :key="tier.tierKey"
                      variant="neutral"
                      size="sm"
                    >
                      {{ tier.displayName }} ({{ tier.count }})
                    </Badge>
                    <span v-if="group.tiers.length === 0" class="text-text-meta text-xs">-</span>
                  </div>
                </td>
                <td>
                  <Badge variant="success" size="sm">Active</Badge>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </Card>
    </template>
  </div>
</template>
