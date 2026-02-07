<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { Card, Button, Badge, Spinner, Alert, SearchBar, Select, Avatar } from '@portal/design-system-vue';
import type { SelectOption } from '@portal/design-system-vue';
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

// --- Search & List ---
const query = ref('');
const users = ref<AdminUserSummary[]>([]);
const totalPages = ref(0);
const totalElements = ref(0);
const currentPage = ref(0);
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

async function loadUsers(page = 0) {
  listLoading.value = true;
  listError.value = '';
  try {
    const result = await searchUsers(query.value, page, pageSize);
    users.value = result.content;
    totalPages.value = result.totalPages;
    totalElements.value = result.totalElements;
    currentPage.value = result.number;
  } catch (err) {
    listError.value = 'Failed to load users.';
    console.error('[Admin] User search failed:', err);
  } finally {
    listLoading.value = false;
  }
}

function handleSearch() {
  selectedUser.value = null;
  loadUsers(0);
}

function handleClear() {
  query.value = '';
  selectedUser.value = null;
  loadUsers(0);
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
    detailError.value = 'Failed to load user details.';
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
    console.error('[Admin] Role assign failed:', err);
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
    console.error('[Admin] Role revoke failed:', err);
  }
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

function formatDate(dateStr: string | null): string {
  if (!dateStr) return '-';
  return new Date(dateStr).toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  });
}

onMounted(async () => {
  await Promise.all([loadUsers(0), fetchRoles().then((r) => (allRoles.value = r))]);
});
</script>

<template>
  <div>
    <h1 class="text-2xl font-bold text-text-heading mb-6">User Management</h1>

    <!-- Search -->
    <div class="mb-4">
      <SearchBar
        v-model="query"
        placeholder="Search by email, username, or nickname..."
        :loading="listLoading"
        @search="handleSearch"
        @clear="handleClear"
      />
    </div>

    <!-- Error -->
    <Alert v-if="listError" variant="error" :title="listError" class="mb-4">
      <template #action>
        <Button variant="ghost" size="sm" @click="loadUsers(currentPage)">Retry</Button>
      </template>
    </Alert>

    <!-- Loading -->
    <div v-if="listLoading && users.length === 0" class="flex justify-center py-12">
      <Spinner size="lg" label="Loading users..." />
    </div>

    <!-- User List Table -->
    <Card v-else variant="outlined" padding="none" class="mb-6">
      <div class="overflow-x-auto">
        <table class="w-full text-sm">
          <thead>
            <tr class="border-b border-border-default bg-bg-elevated">
              <th class="text-left px-4 py-3 font-medium text-text-meta">User</th>
              <th class="text-left px-4 py-3 font-medium text-text-meta">Username</th>
              <th class="text-left px-4 py-3 font-medium text-text-meta">Nickname</th>
              <th class="text-left px-4 py-3 font-medium text-text-meta">Status</th>
              <th class="text-left px-4 py-3 font-medium text-text-meta">Created</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="user in users"
              :key="user.uuid"
              class="border-b border-border-default hover:bg-bg-elevated cursor-pointer transition-colors"
              :class="{ 'bg-bg-elevated': selectedUser?.uuid === user.uuid }"
              @click="selectUser(user)"
            >
              <td class="px-4 py-3">
                <div class="flex items-center gap-3">
                  <Avatar
                    :src="user.profileImageUrl ?? undefined"
                    :name="user.nickname ?? user.email"
                    size="sm"
                  />
                  <span class="text-text-body">{{ user.email }}</span>
                </div>
              </td>
              <td class="px-4 py-3 text-text-body font-mono text-xs">
                {{ user.username ?? '-' }}
              </td>
              <td class="px-4 py-3 text-text-body">
                {{ user.nickname ?? '-' }}
              </td>
              <td class="px-4 py-3">
                <Badge :variant="statusVariant(user.status)" size="sm">
                  {{ user.status }}
                </Badge>
              </td>
              <td class="px-4 py-3 text-text-meta text-xs">
                {{ formatDate(user.createdAt) }}
              </td>
            </tr>
            <tr v-if="users.length === 0 && !listLoading">
              <td colspan="5" class="px-4 py-8 text-center text-text-meta">
                No users found.
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div
        v-if="totalPages > 0"
        class="flex items-center justify-between px-4 py-3 border-t border-border-default"
      >
        <span class="text-xs text-text-meta">
          Page {{ currentPage + 1 }} of {{ totalPages }} ({{ totalElements }} users)
        </span>
        <div class="flex gap-2">
          <Button
            variant="outline"
            size="sm"
            :disabled="currentPage === 0"
            @click="loadUsers(currentPage - 1)"
          >
            Prev
          </Button>
          <Button
            variant="outline"
            size="sm"
            :disabled="currentPage >= totalPages - 1"
            @click="loadUsers(currentPage + 1)"
          >
            Next
          </Button>
        </div>
      </div>
    </Card>

    <!-- Selected User Detail -->
    <template v-if="selectedUser">
      <Card variant="outlined" padding="lg" class="mb-6">
        <!-- User Header -->
        <div class="flex items-center gap-4 mb-6">
          <Avatar
            :src="selectedUser.profileImageUrl ?? undefined"
            :name="selectedUser.nickname ?? selectedUser.email"
            size="lg"
          />
          <div>
            <div class="text-lg font-semibold text-text-heading">{{ selectedUser.email }}</div>
            <div class="text-xs text-text-meta font-mono mt-1">{{ selectedUser.uuid }}</div>
            <Badge :variant="statusVariant(selectedUser.status)" size="sm" class="mt-1">
              {{ selectedUser.status }}
            </Badge>
          </div>
        </div>

        <div v-if="detailLoading" class="flex justify-center py-8">
          <Spinner size="md" label="Loading details..." />
        </div>

        <Alert v-else-if="detailError" variant="error" :title="detailError" />

        <template v-else>
          <!-- Assigned Roles -->
          <section class="mb-6">
            <h2 class="font-semibold text-text-heading mb-3">Assigned Roles</h2>
            <div class="flex flex-wrap gap-2 mb-3">
              <div
                v-for="role in userRoles"
                :key="role.id"
                class="flex items-center gap-1"
              >
                <Badge variant="primary" size="sm">{{ role.displayName }}</Badge>
                <Button variant="ghost" size="xs" @click="handleRevoke(role.roleKey)">
                  <span class="text-status-error text-xs">Revoke</span>
                </Button>
              </div>
              <span v-if="userRoles.length === 0" class="text-text-meta text-sm">
                No roles assigned
              </span>
            </div>

            <!-- Role Assignment -->
            <div class="flex items-end gap-2">
              <div class="flex-1 max-w-sm">
                <Select
                  v-model="selectedRoleKey"
                  :options="roleOptions"
                  placeholder="Select a role to assign..."
                  searchable
                  clearable
                  size="sm"
                />
              </div>
              <Button
                variant="primary"
                size="sm"
                :disabled="!selectedRoleKey"
                :loading="assignLoading"
                @click="handleAssign"
              >
                Assign
              </Button>
            </div>
          </section>

          <!-- Effective Permissions -->
          <section class="mb-6">
            <h2 class="font-semibold text-text-heading mb-3">Effective Permissions</h2>
            <div class="flex flex-wrap gap-1">
              <Badge
                v-for="perm in userPermissions?.permissions"
                :key="perm"
                variant="default"
                size="sm"
              >
                {{ perm }}
              </Badge>
              <span
                v-if="!userPermissions?.permissions?.length"
                class="text-text-meta text-sm"
              >
                No permissions
              </span>
            </div>
          </section>

          <!-- Memberships -->
          <section>
            <h2 class="font-semibold text-text-heading mb-3">Memberships</h2>
            <div class="flex flex-wrap gap-2">
              <div v-for="m in userMemberships" :key="m.id" class="flex items-center gap-1">
                <Badge variant="info" size="sm">{{ m.membershipGroup }}</Badge>
                <span class="text-text-body text-sm">{{ m.tierDisplayName }} ({{ m.tierKey }})</span>
              </div>
              <span v-if="userMemberships.length === 0" class="text-text-meta text-sm">
                No memberships
              </span>
            </div>
          </section>
        </template>
      </Card>
    </template>
  </div>
</template>
