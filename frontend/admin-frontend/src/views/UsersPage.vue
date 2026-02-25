<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { Badge, Spinner, Alert, Select, Avatar, Tag, Button, Tooltip, useApiError, useToast } from '@portal/design-vue';
import type { SelectOption } from '@portal/design-vue';
import {
  searchUsers,
  fetchRoles,
  fetchUserRoles,
  fetchUserPermissions,
  fetchUserMemberships,
  fetchAuditLogs,
  assignRole,
  revokeRole,
  fetchMembershipGroups,
  fetchMembershipTiers,
  changeUserMembership,
} from '@/api/admin';
import type {
  AdminUserSummary,
  RoleResponse,
  UserRole,
  UserPermissions,
  MembershipResponse,
  MembershipTierResponse,
  AuditLog,
} from '@/dto/admin';

const { getErrorMessage, handleError } = useApiError();
const toast = useToast();

// --- Search & List ---
const query = ref('');
const users = ref<AdminUserSummary[]>([]);
const totalPages = ref(0);
const totalElements = ref(0);
const currentPage = ref(1);
const pageSize = 20;
const listLoading = ref(false);
const listError = ref('');

// --- All roles (for assignment dropdown) ---
const allRoles = ref<RoleResponse[]>([]);

// --- Selected user detail ---
const selectedUser = ref<AdminUserSummary | null>(null);
const userRoles = ref<UserRole[]>([]);
const userPermissions = ref<UserPermissions | null>(null);
const userMemberships = ref<MembershipResponse[]>([]);
const detailLoading = ref(false);
const detailError = ref('');

// --- Audit logs ---
const userAuditLogs = ref<AuditLog[]>([]);

// --- Role assignment ---
const selectedRoleKey = ref<string | number | null>(null);
const assignLoading = ref(false);

// --- Membership tiers (for tier change) ---
const membershipGroups = ref<string[]>([]);
const allGroupTiers = ref<Record<string, MembershipTierResponse[]>>({});
const changingMembershipGroup = ref<string | null>(null);

async function loadUsers(page = 1) {
  listLoading.value = true;
  listError.value = '';
  try {
    const result = await searchUsers(query.value, page, pageSize);
    users.value = result.items;
    totalPages.value = result.totalPages;
    totalElements.value = result.totalElements;
    currentPage.value = result.page;
  } catch (err) {
    listError.value = getErrorMessage(err, 'Failed to load users.');
    console.error('[Admin] User search failed:', err);
  } finally {
    listLoading.value = false;
  }
}

function handleSearch() {
  selectedUser.value = null;
  loadUsers(1);
}

function handleClear() {
  query.value = '';
  selectedUser.value = null;
  loadUsers(1);
}

async function selectUser(user: AdminUserSummary) {
  selectedUser.value = user;
  detailLoading.value = true;
  detailError.value = '';
  selectedRoleKey.value = null;
  try {
    const [roles, perms, memberships, audit] = await Promise.all([
      fetchUserRoles(user.uuid),
      fetchUserPermissions(user.uuid),
      fetchUserMemberships(user.uuid),
      fetchAuditLogs(1, 5, user.uuid),
    ]);
    userRoles.value = roles;
    userPermissions.value = perms;
    userMemberships.value = memberships;
    userAuditLogs.value = audit.items;
  } catch (err) {
    detailError.value = getErrorMessage(err, 'Failed to load user details.');
    console.error('[Admin] User detail load failed:', err);
  } finally {
    detailLoading.value = false;
  }
}

const roleOptions = computed<SelectOption[]>(() => {
  const assigned = new Set(userRoles.value.map((r) => r.roleKey));
  return allRoles.value
    .filter((r) => r.active && !assigned.has(r.roleKey))
    .map((r) => ({ value: r.roleKey, label: `${r.displayName} (${r.roleKey})` }));
});

async function handleAssign() {
  if (!selectedRoleKey.value || !selectedUser.value) return;
  assignLoading.value = true;
  try {
    await assignRole(selectedUser.value.uuid, String(selectedRoleKey.value));
    selectedRoleKey.value = null;
    await selectUser(selectedUser.value);
  } catch (err) {
    handleError(err, 'Failed to assign role');
  } finally {
    assignLoading.value = false;
  }
}

async function handleRevoke(roleKey: string) {
  if (!selectedUser.value) return;
  try {
    await revokeRole(selectedUser.value.uuid, roleKey);
    await selectUser(selectedUser.value);
  } catch (err) {
    handleError(err, 'Failed to revoke role');
  }
}

function statusColor(status: string): string {
  const map: Record<string, string> = {
    ACTIVE: 'bg-status-success',
    DORMANT: 'bg-status-warning',
    BANNED: 'bg-status-error',
    WITHDRAWAL_PENDING: 'bg-status-info',
  };
  return map[status] ?? 'bg-text-muted';
}

function statusVariant(status: string): 'success' | 'warning' | 'danger' | 'info' | 'default' {
  const map: Record<string, 'success' | 'warning' | 'danger' | 'info'> = {
    ACTIVE: 'success',
    DORMANT: 'warning',
    BANNED: 'danger',
    WITHDRAWAL_PENDING: 'info',
  };
  return map[status] ?? 'default';
}

function timeAgo(dateStr: string | null): string {
  if (!dateStr) return 'Never';
  const diff = Date.now() - new Date(dateStr).getTime();
  const mins = Math.floor(diff / 60_000);
  if (mins < 1) return 'Just now';
  if (mins < 60) return `${mins}m ago`;
  const hours = Math.floor(mins / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  return `${days}d ago`;
}

async function copyUuid() {
  if (!selectedUser.value) return;
  try {
    await navigator.clipboard.writeText(selectedUser.value.uuid);
    toast.success('UUID copied to clipboard');
  } catch {
    const el = document.createElement('textarea');
    el.value = selectedUser.value.uuid;
    document.body.appendChild(el);
    el.select();
    document.execCommand('copy');
    document.body.removeChild(el);
    toast.success('UUID copied to clipboard');
  }
}

function membershipForGroup(group: string): MembershipResponse | undefined {
  return userMemberships.value.find((m) => m.membershipGroup === group);
}

function tierOptionsForGroup(group: string, currentTierKey: string): SelectOption[] {
  const tiers = allGroupTiers.value[group] ?? [];
  return tiers
    .filter((t) => t.tierKey !== currentTierKey)
    .map((t) => ({ value: t.tierKey, label: `${t.displayName} (${t.tierKey})` }));
}

async function handleChangeMembership(group: string, tierKey: string | null) {
  if (!selectedUser.value || !tierKey) return;
  changingMembershipGroup.value = group;
  try {
    await changeUserMembership(selectedUser.value.uuid, group, String(tierKey));
    toast.success(`Tier changed to ${tierKey} for ${group}`);
    userMemberships.value = await fetchUserMemberships(selectedUser.value.uuid);
  } catch (err) {
    handleError(err, 'Failed to change membership tier');
  } finally {
    changingMembershipGroup.value = null;
  }
}

async function loadAllMembershipTiers() {
  try {
    const groups = await fetchMembershipGroups();
    membershipGroups.value = groups;
    const results = await Promise.all(groups.map((g) => fetchMembershipTiers(g)));
    groups.forEach((g, i) => {
      allGroupTiers.value[g] = results[i]!;
    });
  } catch {
    // graceful degradation - tier change won't be available
  }
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

onMounted(async () => {
  await Promise.all([
    loadUsers(1),
    fetchRoles().then((r) => (allRoles.value = r)),
    loadAllMembershipTiers(),
  ]);
});
</script>

<template>
  <div>
    <!-- Header with search -->
    <div class="flex items-center justify-between mb-6">
      <h1 class="text-2xl font-bold text-text-heading">Users</h1>
      <div class="flex items-center gap-2">
        <div class="relative">
          <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-text-muted" style="font-size: 18px;">search</span>
          <input
            v-model="query"
            type="text"
            placeholder="Search users..."
            class="pl-9 pr-3 py-2 w-64 bg-bg-card border border-border-default rounded-lg text-sm text-text-body placeholder:text-text-muted focus:outline-none focus:border-border-focus"
            @keyup.enter="handleSearch"
          />
        </div>
        <Button
          v-if="query"
          variant="ghost"
          size="xs"
          @click="handleClear"
        >
          Clear
        </Button>
      </div>
    </div>

    <!-- Error -->
    <Alert v-if="listError" variant="error" :title="listError" class="mb-4">
      <template #action>
        <Button variant="ghost" size="xs" class="underline" @click="loadUsers(currentPage)">Retry</Button>
      </template>
    </Alert>

    <!-- Loading -->
    <div v-if="listLoading && users.length === 0" class="flex justify-center py-12">
      <Spinner size="lg" label="Loading users..." />
    </div>

    <!-- Master-Detail Split -->
    <div v-else class="flex gap-6">
      <!-- User List (Left Panel) -->
      <div class="w-2/5 shrink-0">
        <div class="bg-bg-card border border-border-default rounded-lg overflow-hidden">
          <div class="max-h-[calc(100vh-220px)] overflow-y-auto">
            <div
              v-for="user in users"
              :key="user.uuid"
              @click="selectUser(user)"
              :class="[
                'flex items-center gap-3 px-4 py-3 cursor-pointer transition-colors border-b border-border-muted last:border-b-0',
                selectedUser?.uuid === user.uuid
                  ? 'bg-brand-primary/5 border-l-2 border-l-brand-primary'
                  : 'hover:bg-bg-hover border-l-2 border-l-transparent'
              ]"
            >
              <Avatar
                :src="user.profileImageUrl ?? undefined"
                :name="user.nickname ?? user.email"
                size="sm"
              />
              <div class="flex-1 min-w-0">
                <div class="flex items-center gap-2">
                  <span class="text-sm font-medium text-text-heading truncate">
                    {{ user.nickname ?? user.email }}
                  </span>
                  <span :class="['w-1.5 h-1.5 rounded-full shrink-0', statusColor(user.status)]"></span>
                </div>
                <div class="text-xs text-text-meta truncate">{{ user.email }}</div>
              </div>
              <div class="text-[11px] text-text-muted whitespace-nowrap">
                {{ timeAgo(user.lastLoginAt) }}
              </div>
            </div>

            <div v-if="users.length === 0 && !listLoading" class="px-4 py-8 text-center text-text-meta text-sm">
              No users found
            </div>
          </div>

          <!-- Pagination -->
          <div
            v-if="totalPages > 0"
            class="flex items-center justify-between px-4 py-2.5 border-t border-border-default bg-bg-elevated text-xs"
          >
            <span class="text-text-meta">{{ totalElements }} users</span>
            <div class="flex items-center gap-1">
              <Button
                variant="ghost"
                size="xs"
                :disabled="currentPage === 1"
                @click="loadUsers(currentPage - 1)"
              >
                Prev
              </Button>
              <span class="text-text-meta px-2">{{ currentPage }} / {{ totalPages }}</span>
              <Button
                variant="ghost"
                size="xs"
                :disabled="currentPage >= totalPages"
                @click="loadUsers(currentPage + 1)"
              >
                Next
              </Button>
            </div>
          </div>
        </div>
      </div>

      <!-- User Detail (Right Panel) -->
      <div class="flex-1 min-w-0">
        <template v-if="!selectedUser">
          <div class="flex flex-col items-center justify-center py-20 text-text-muted">
            <span class="material-symbols-outlined mb-3" style="font-size: 48px; opacity: 0.3;">person_search</span>
            <p class="text-sm">Select a user to view details</p>
          </div>
        </template>

        <template v-else>
          <div class="bg-bg-card border border-border-default rounded-lg">
            <!-- Profile Header -->
            <div class="p-5 border-b border-border-default">
              <div class="flex items-start gap-4">
                <Avatar
                  :src="selectedUser.profileImageUrl ?? undefined"
                  :name="selectedUser.nickname ?? selectedUser.email"
                  size="lg"
                />
                <div class="flex-1 min-w-0">
                  <div class="flex items-center justify-between">
                    <div class="text-lg font-semibold text-text-heading">
                      {{ selectedUser.nickname ?? selectedUser.email }}
                    </div>
                    <div class="flex gap-1">
                      <Tooltip content="Edit User">
                        <Button variant="ghost" size="xs">
                          <span class="material-symbols-outlined" style="font-size: 20px;">edit</span>
                        </Button>
                      </Tooltip>
                      <Tooltip content="Delete User">
                        <Button variant="ghost" size="xs" class="!text-text-muted hover:!text-status-error hover:!bg-status-error/10">
                          <span class="material-symbols-outlined" style="font-size: 20px;">delete</span>
                        </Button>
                      </Tooltip>
                    </div>
                  </div>
                  <div class="text-sm text-text-meta">{{ selectedUser.email }}</div>
                  <div class="flex items-center gap-3 mt-2">
                    <Badge :variant="statusVariant(selectedUser.status)" size="sm">
                      {{ selectedUser.status }}
                    </Badge>
                    <Tooltip content="Click to copy full UUID">
                      <button
                        class="inline-flex items-center gap-1.5 px-2 py-0.5 bg-bg-elevated border border-border-default rounded text-xs font-mono text-text-muted hover:text-text-body transition-colors"
                        @click="copyUuid"
                      >
                        <span class="truncate max-w-[180px]">{{ selectedUser.uuid }}</span>
                        <span class="material-symbols-outlined shrink-0" style="font-size: 14px;">content_copy</span>
                      </button>
                    </Tooltip>
                  </div>
                </div>
              </div>
            </div>

            <div v-if="detailLoading" class="flex justify-center py-8">
              <Spinner size="md" label="Loading details..." />
            </div>

            <Alert v-else-if="detailError" variant="error" :title="detailError" class="m-5" />

            <template v-else>
              <!-- Assigned Roles -->
              <div class="p-5 border-b border-border-default">
                <h3 class="text-sm font-semibold text-text-heading mb-3 flex items-center gap-2">
                  <span class="material-symbols-outlined" style="font-size: 16px;">shield</span>
                  Assigned Roles
                </h3>
                <div class="flex flex-wrap gap-2 mb-3">
                  <Tag
                    v-for="role in userRoles"
                    :key="role.id"
                    variant="solid"
                    size="md"
                    removable
                    @remove="handleRevoke(role.roleKey)"
                  >
                    {{ role.displayName }}
                  </Tag>
                  <span v-if="userRoles.length === 0" class="text-text-meta text-sm">No roles assigned</span>
                </div>
                <div class="flex items-end gap-2">
                  <Select
                    v-model="selectedRoleKey"
                    :options="roleOptions"
                    placeholder="Assign a role..."
                    searchable
                    clearable
                    size="sm"
                    class="flex-1"
                  />
                  <Button
                    variant="primary"
                    size="sm"
                    :disabled="!selectedRoleKey || assignLoading"
                    :loading="assignLoading"
                    @click="handleAssign"
                  >
                    Assign
                  </Button>
                </div>
              </div>

              <!-- Effective Permissions -->
              <div class="p-5 border-b border-border-default">
                <h3 class="text-sm font-semibold text-text-heading mb-3 flex items-center gap-2">
                  <span class="material-symbols-outlined" style="font-size: 16px;">verified_user</span>
                  Effective Permissions
                </h3>
                <div class="space-y-1">
                  <div
                    v-for="perm in userPermissions?.permissions"
                    :key="perm"
                    class="flex items-center gap-2 text-sm"
                  >
                    <span class="material-symbols-outlined text-status-success" style="font-size: 16px;">check_circle</span>
                    <span class="font-mono text-xs text-text-body">{{ perm }}</span>
                  </div>
                  <p v-if="!userPermissions?.permissions?.length" class="text-text-meta text-sm">
                    No permissions
                  </p>
                </div>
              </div>

              <!-- Context Memberships -->
              <div class="p-5 border-b border-border-default">
                <h3 class="text-sm font-semibold text-text-heading mb-3 flex items-center gap-2">
                  <span class="material-symbols-outlined" style="font-size: 16px;">card_membership</span>
                  Memberships
                </h3>
                <div class="grid grid-cols-1 gap-2">
                  <div
                    v-for="group in membershipGroups"
                    :key="group"
                    class="flex items-center justify-between p-3 bg-bg-elevated rounded-lg"
                  >
                    <div>
                      <div class="text-sm font-medium text-text-heading">{{ group }}</div>
                      <div v-if="membershipForGroup(group)" class="flex items-center gap-2 mt-1">
                        <Badge variant="info" size="sm">
                          {{ membershipForGroup(group)!.tierDisplayName }}
                        </Badge>
                        <Badge
                          :variant="membershipForGroup(group)!.status === 'ACTIVE' ? 'success' : membershipForGroup(group)!.status === 'CANCELLED' ? 'danger' : 'warning'"
                          size="sm"
                        >
                          {{ membershipForGroup(group)!.status }}
                        </Badge>
                      </div>
                      <div v-else class="text-xs text-text-meta mt-1">No membership</div>
                    </div>
                    <div class="flex items-center gap-2 min-w-[180px]">
                      <Select
                        :options="tierOptionsForGroup(group, membershipForGroup(group)?.tierKey ?? '')"
                        placeholder="Change tier..."
                        size="sm"
                        clearable
                        class="flex-1"
                        @update:model-value="(val) => handleChangeMembership(group, String(val))"
                      />
                      <Spinner v-if="changingMembershipGroup === group" size="sm" />
                    </div>
                  </div>
                  <p v-if="membershipGroups.length === 0" class="text-text-meta text-sm">
                    No membership groups
                  </p>
                </div>
              </div>

              <!-- Recent Activity -->
              <div class="p-5">
                <h3 class="text-sm font-semibold text-text-heading mb-3 flex items-center gap-2">
                  <span class="material-symbols-outlined" style="font-size: 16px;">history</span>
                  Recent Activity
                </h3>
                <div v-if="userAuditLogs.length === 0" class="text-sm text-text-meta text-center py-4">
                  No recent activity
                </div>
                <div v-else class="admin-timeline">
                  <div
                    v-for="log in userAuditLogs"
                    :key="log.id"
                    class="admin-timeline-item"
                  >
                    <div class="timeline-time">{{ log.createdAt ? timeAgo(log.createdAt) : '-' }}</div>
                    <div class="timeline-content">
                      <span :class="['event-badge', eventBadgeClass(log.eventType)]">
                        {{ formatEventType(log.eventType) }}
                      </span>
                      <span class="ml-2">{{ log.details }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </template>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>
