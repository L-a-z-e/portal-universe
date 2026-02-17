<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { Badge, Spinner, Alert, Select, Avatar, useApiError } from '@portal/design-vue';
import type { SelectOption } from '@portal/design-vue';
import {
  searchUsers,
  fetchRoles,
  fetchUserRoles,
  fetchUserPermissions,
  fetchUserMemberships,
  assignRole,
  revokeRole,
} from '@/api/admin';
import type {
  AdminUserSummary,
  RoleResponse,
  UserRole,
  UserPermissions,
  MembershipResponse,
} from '@/dto/admin';

const { getErrorMessage, handleError } = useApiError();

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

// --- Role assignment ---
const selectedRoleKey = ref<string | number | null>(null);
const assignLoading = ref(false);

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
    const [roles, perms, memberships] = await Promise.all([
      fetchUserRoles(user.uuid),
      fetchUserPermissions(user.uuid),
      fetchUserMemberships(user.uuid),
    ]);
    userRoles.value = roles;
    userPermissions.value = perms;
    userMemberships.value = memberships;
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

function formatDate(dateStr: string | null): string {
  if (!dateStr) return '-';
  return new Date(dateStr).toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  });
}

onMounted(async () => {
  await Promise.all([loadUsers(1), fetchRoles().then((r) => (allRoles.value = r))]);
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
        <button
          v-if="query"
          @click="handleClear"
          class="text-xs text-text-meta hover:text-text-body"
        >
          Clear
        </button>
      </div>
    </div>

    <!-- Error -->
    <Alert v-if="listError" variant="error" :title="listError" class="mb-4">
      <template #action>
        <button class="text-sm underline" @click="loadUsers(currentPage)">Retry</button>
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
              <button
                :disabled="currentPage === 1"
                @click="loadUsers(currentPage - 1)"
                class="px-2 py-1 rounded hover:bg-bg-hover disabled:opacity-30 text-text-body"
              >
                Prev
              </button>
              <span class="text-text-meta px-2">{{ currentPage }} / {{ totalPages }}</span>
              <button
                :disabled="currentPage >= totalPages"
                @click="loadUsers(currentPage + 1)"
                class="px-2 py-1 rounded hover:bg-bg-hover disabled:opacity-30 text-text-body"
              >
                Next
              </button>
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
              <div class="flex items-center gap-4">
                <Avatar
                  :src="selectedUser.profileImageUrl ?? undefined"
                  :name="selectedUser.nickname ?? selectedUser.email"
                  size="lg"
                />
                <div class="flex-1">
                  <div class="text-lg font-semibold text-text-heading">
                    {{ selectedUser.nickname ?? selectedUser.email }}
                  </div>
                  <div class="text-sm text-text-meta">{{ selectedUser.email }}</div>
                  <div class="flex items-center gap-2 mt-1.5">
                    <Badge :variant="statusVariant(selectedUser.status)" size="sm">
                      {{ selectedUser.status }}
                    </Badge>
                    <span class="text-xs text-text-muted font-mono">{{ selectedUser.uuid.slice(0, 8) }}...</span>
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
                  <span
                    v-for="role in userRoles"
                    :key="role.id"
                    class="inline-flex items-center gap-1 px-2.5 py-1 bg-brand-primary/10 text-brand-primary rounded text-xs font-medium"
                  >
                    {{ role.displayName }}
                    <button
                      @click="handleRevoke(role.roleKey)"
                      class="ml-0.5 hover:text-status-error transition-colors"
                      title="Revoke"
                    >
                      &times;
                    </button>
                  </span>
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
                  <button
                    :disabled="!selectedRoleKey || assignLoading"
                    @click="handleAssign"
                    class="px-3 py-2 bg-brand-primary text-white rounded text-xs font-medium hover:bg-brand-primaryHover disabled:opacity-40 transition-colors"
                  >
                    Assign
                  </button>
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
              <div class="p-5">
                <h3 class="text-sm font-semibold text-text-heading mb-3 flex items-center gap-2">
                  <span class="material-symbols-outlined" style="font-size: 16px;">card_membership</span>
                  Memberships
                </h3>
                <div class="grid grid-cols-1 gap-2">
                  <div
                    v-for="m in userMemberships"
                    :key="m.id"
                    class="flex items-center justify-between p-3 bg-bg-elevated rounded-lg"
                  >
                    <div>
                      <div class="text-sm font-medium text-text-heading">{{ m.membershipGroup }}</div>
                      <div class="text-xs text-text-meta">{{ m.tierDisplayName }} ({{ m.tierKey }})</div>
                    </div>
                    <Badge
                      :variant="m.status === 'ACTIVE' ? 'success' : m.status === 'CANCELLED' ? 'danger' : 'warning'"
                      size="sm"
                    >
                      {{ m.status }}
                    </Badge>
                  </div>
                  <p v-if="userMemberships.length === 0" class="text-text-meta text-sm">
                    No memberships
                  </p>
                </div>
              </div>
            </template>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>
