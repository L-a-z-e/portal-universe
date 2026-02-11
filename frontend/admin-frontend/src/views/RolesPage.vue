<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { Card, Button, Badge, Spinner, Alert, SearchBar, Select, Input, useApiError } from '@portal/design-system-vue';
import type { SelectOption } from '@portal/design-system-vue';
import {
  fetchRoles,
  fetchRoleDetail,
  createRole,
  updateRole,
  toggleRoleActive,
  fetchPermissions,
  assignPermission,
  removePermission,
} from '@/api/admin';
import type {
  RoleResponse,
  RoleDetailResponse,
  PermissionResponse,
  CreateRoleRequest,
} from '@/dto/admin';

const { getErrorMessage } = useApiError();

// === State ===
const roles = ref<RoleResponse[]>([]);
const allPermissions = ref<PermissionResponse[]>([]);
const loading = ref(true);
const error = ref('');

// Search
const searchQuery = ref('');

// Selected role detail
const selectedRole = ref<RoleDetailResponse | null>(null);
const detailLoading = ref(false);

// Edit mode
const editMode = ref(false);
const editDisplayName = ref('');
const editDescription = ref('');
const editSaving = ref(false);

// Create mode
const createMode = ref(false);
const createForm = ref<CreateRoleRequest>({
  roleKey: '',
  displayName: '',
  description: '',
  serviceScope: '',
  membershipGroup: '',
  parentRoleKey: '',
});
const createSaving = ref(false);
const createError = ref('');

// Permission assignment
const selectedPermissionKey = ref<string | null>(null);
const permissionAssigning = ref(false);

// === Computed ===
const filteredRoles = computed(() => {
  if (!searchQuery.value) return roles.value;
  const q = searchQuery.value.toLowerCase();
  return roles.value.filter(
    (r) => r.roleKey.toLowerCase().includes(q) || r.displayName.toLowerCase().includes(q),
  );
});

const parentRoleOptions = computed<SelectOption[]>(() =>
  roles.value
    .filter((r) => r.active)
    .map((r) => ({ value: r.roleKey, label: `${r.displayName} (${r.roleKey})` })),
);

const permissionOptions = computed<SelectOption[]>(() => {
  const assigned = new Set(selectedRole.value?.permissions.map((p) => p.permissionKey) ?? []);
  return allPermissions.value
    .filter((p) => p.active && !assigned.has(p.permissionKey))
    .map((p) => ({
      value: p.permissionKey,
      label: `${p.permissionKey}${p.description ? ` - ${p.description}` : ''}`,
    }));
});

// === Actions ===
async function loadRoles() {
  loading.value = true;
  error.value = '';
  try {
    roles.value = await fetchRoles();
  } catch (e: unknown) {
    error.value = getErrorMessage(e, 'Failed to load roles');
  } finally {
    loading.value = false;
  }
}

async function loadPermissions() {
  try {
    allPermissions.value = await fetchPermissions();
  } catch {
    // non-critical
  }
}

async function selectRole(roleKey: string) {
  createMode.value = false;
  editMode.value = false;
  detailLoading.value = true;
  error.value = '';
  try {
    selectedRole.value = await fetchRoleDetail(roleKey);
  } catch (e: unknown) {
    error.value = getErrorMessage(e, 'Failed to load role detail');
  } finally {
    detailLoading.value = false;
  }
}

function enterEditMode() {
  if (!selectedRole.value) return;
  editDisplayName.value = selectedRole.value.displayName;
  editDescription.value = selectedRole.value.description ?? '';
  editMode.value = true;
}

async function saveEdit() {
  if (!selectedRole.value) return;
  editSaving.value = true;
  try {
    await updateRole(selectedRole.value.roleKey, {
      displayName: editDisplayName.value,
      description: editDescription.value,
    });
    editMode.value = false;
    await selectRole(selectedRole.value.roleKey);
    await loadRoles();
  } catch (e: unknown) {
    error.value = getErrorMessage(e, 'Failed to update role');
  } finally {
    editSaving.value = false;
  }
}

async function handleToggleActive() {
  if (!selectedRole.value) return;
  try {
    await toggleRoleActive(selectedRole.value.roleKey, !selectedRole.value.active);
    await selectRole(selectedRole.value.roleKey);
    await loadRoles();
  } catch (e: unknown) {
    error.value = getErrorMessage(e, 'Failed to toggle role status');
  }
}

function enterCreateMode() {
  selectedRole.value = null;
  editMode.value = false;
  createError.value = '';
  createForm.value = {
    roleKey: '',
    displayName: '',
    description: '',
    serviceScope: '',
    membershipGroup: '',
    parentRoleKey: '',
  };
  createMode.value = true;
}

async function handleCreate() {
  createSaving.value = true;
  createError.value = '';
  try {
    const payload: CreateRoleRequest = {
      roleKey: createForm.value.roleKey,
      displayName: createForm.value.displayName,
    };
    if (createForm.value.description) payload.description = createForm.value.description;
    if (createForm.value.serviceScope) payload.serviceScope = createForm.value.serviceScope;
    if (createForm.value.membershipGroup) payload.membershipGroup = createForm.value.membershipGroup;
    if (createForm.value.parentRoleKey) payload.parentRoleKey = createForm.value.parentRoleKey;

    await createRole(payload);
    createMode.value = false;
    await loadRoles();
  } catch (e: unknown) {
    createError.value = getErrorMessage(e, 'Failed to create role');
  } finally {
    createSaving.value = false;
  }
}

async function handleAssignPermission() {
  if (!selectedRole.value || !selectedPermissionKey.value) return;
  permissionAssigning.value = true;
  try {
    await assignPermission(selectedRole.value.roleKey, selectedPermissionKey.value);
    selectedPermissionKey.value = null;
    await selectRole(selectedRole.value.roleKey);
  } catch (e: unknown) {
    error.value = getErrorMessage(e, 'Failed to assign permission');
  } finally {
    permissionAssigning.value = false;
  }
}

async function handleRemovePermission(permissionKey: string) {
  if (!selectedRole.value) return;
  try {
    await removePermission(selectedRole.value.roleKey, permissionKey);
    await selectRole(selectedRole.value.roleKey);
  } catch (e: unknown) {
    error.value = getErrorMessage(e, 'Failed to remove permission');
  }
}

onMounted(() => {
  loadRoles();
  loadPermissions();
});
</script>

<template>
  <div>
    <h1 class="text-2xl font-bold text-text-heading mb-6">Role Management</h1>

    <!-- Toolbar -->
    <div class="flex items-center justify-between gap-4 mb-4">
      <SearchBar v-model="searchQuery" placeholder="Search roles..." class="flex-1 max-w-sm" />
      <Button variant="primary" @click="enterCreateMode">Create Role</Button>
    </div>

    <!-- Error -->
    <Alert v-if="error" variant="error" dismissible class="mb-4" @dismiss="error = ''">
      {{ error }}
    </Alert>

    <!-- Loading -->
    <div v-if="loading" class="flex justify-center py-12">
      <Spinner size="lg" />
    </div>

    <!-- Roles Table -->
    <Card v-else variant="outlined" padding="none" class="mb-6">
      <table class="w-full text-sm">
        <thead class="bg-bg-elevated border-b border-border-default">
          <tr>
            <th class="text-left p-3 text-text-heading">Role Key</th>
            <th class="text-left p-3 text-text-heading">Display Name</th>
            <th class="text-left p-3 text-text-heading">Scope</th>
            <th class="text-left p-3 text-text-heading">Parent</th>
            <th class="text-left p-3 text-text-heading">System</th>
            <th class="text-left p-3 text-text-heading">Status</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="filteredRoles.length === 0">
            <td colspan="6" class="p-4 text-center text-text-meta">No roles found</td>
          </tr>
          <tr
            v-for="role in filteredRoles"
            :key="role.id"
            class="border-b border-border-default hover:bg-bg-elevated transition-colors cursor-pointer"
            :class="{ 'bg-bg-elevated': selectedRole?.roleKey === role.roleKey }"
            @click="selectRole(role.roleKey)"
          >
            <td class="p-3 font-mono text-text-body">{{ role.roleKey }}</td>
            <td class="p-3 text-text-body">{{ role.displayName }}</td>
            <td class="p-3 text-text-meta">{{ role.serviceScope ?? '-' }}</td>
            <td class="p-3 text-text-meta">{{ role.parentRoleKey ?? '-' }}</td>
            <td class="p-3">
              <Badge v-if="role.system" variant="info" size="sm">System</Badge>
              <span v-else class="text-text-meta">-</span>
            </td>
            <td class="p-3">
              <Badge :variant="role.active ? 'success' : 'danger'" size="sm">
                {{ role.active ? 'Active' : 'Inactive' }}
              </Badge>
            </td>
          </tr>
        </tbody>
      </table>
    </Card>

    <!-- Create Role Panel -->
    <Card v-if="createMode" variant="outlined" padding="lg" class="mb-6">
      <h2 class="text-lg font-semibold text-text-heading mb-4">Create New Role</h2>

      <Alert v-if="createError" variant="error" dismissible class="mb-4" @dismiss="createError = ''">
        {{ createError }}
      </Alert>

      <div class="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
        <Input
          v-model="createForm.roleKey"
          label="Role Key"
          placeholder="ROLE_CUSTOM_NAME"
          required
        />
        <Input
          v-model="createForm.displayName"
          label="Display Name"
          placeholder="Custom Role Name"
          required
        />
        <Input v-model="createForm.description" label="Description" placeholder="Role description" />
        <Input v-model="createForm.serviceScope" label="Service Scope" placeholder="e.g. shopping" />
        <Input
          v-model="createForm.membershipGroup"
          label="Membership Group"
          placeholder="e.g. USER_SHOPPING"
        />
        <Select
          v-model="createForm.parentRoleKey"
          :options="parentRoleOptions"
          label="Parent Role"
          placeholder="Select parent role (optional)"
          clearable
        />
      </div>

      <div class="flex gap-2">
        <Button variant="primary" :loading="createSaving" @click="handleCreate">Create</Button>
        <Button variant="ghost" @click="createMode = false">Cancel</Button>
      </div>
    </Card>

    <!-- Role Detail Panel -->
    <Card v-if="selectedRole || detailLoading" variant="outlined" padding="lg">
      <div v-if="detailLoading" class="flex justify-center py-8">
        <Spinner size="md" />
      </div>

      <template v-else-if="selectedRole">
        <!-- Header -->
        <div class="flex items-start justify-between mb-4">
          <div>
            <h2 class="text-lg font-semibold text-text-heading">{{ selectedRole.displayName }}</h2>
            <p class="font-mono text-sm text-text-meta">{{ selectedRole.roleKey }}</p>
          </div>
          <div class="flex items-center gap-2">
            <Badge v-if="selectedRole.system" variant="info" size="sm">System</Badge>
            <Badge :variant="selectedRole.active ? 'success' : 'danger'" size="sm">
              {{ selectedRole.active ? 'Active' : 'Inactive' }}
            </Badge>
          </div>
        </div>

        <!-- Edit Mode -->
        <div v-if="editMode" class="mb-4 space-y-3">
          <Input v-model="editDisplayName" label="Display Name" required />
          <Input v-model="editDescription" label="Description" />
          <div class="flex gap-2">
            <Button variant="primary" size="sm" :loading="editSaving" @click="saveEdit">Save</Button>
            <Button variant="ghost" size="sm" @click="editMode = false">Cancel</Button>
          </div>
        </div>

        <!-- Actions -->
        <div v-else class="flex gap-2 mb-4">
          <Button variant="outline" size="sm" @click="enterEditMode">Edit</Button>
          <Button
            v-if="!selectedRole.system"
            :variant="selectedRole.active ? 'danger' : 'primary'"
            size="sm"
            @click="handleToggleActive"
          >
            {{ selectedRole.active ? 'Deactivate' : 'Activate' }}
          </Button>
        </div>

        <!-- Description -->
        <p v-if="selectedRole.description && !editMode" class="text-sm text-text-body mb-4">
          {{ selectedRole.description }}
        </p>

        <!-- Permissions Section -->
        <section class="mb-4">
          <h3 class="text-sm font-semibold text-text-heading mb-2">Permissions</h3>

          <div v-if="selectedRole.permissions.length === 0" class="text-sm text-text-meta mb-2">
            No permissions assigned
          </div>

          <div class="flex flex-wrap gap-2 mb-3">
            <span
              v-for="perm in selectedRole.permissions"
              :key="perm.id"
              class="inline-flex items-center gap-1"
            >
              <Badge variant="outline" size="sm">{{ perm.permissionKey }}</Badge>
              <button
                class="text-text-meta hover:text-status-error transition-colors text-xs"
                title="Remove permission"
                @click="handleRemovePermission(perm.permissionKey)"
              >
                &times;
              </button>
            </span>
          </div>

          <div class="flex items-end gap-2">
            <Select
              v-model="selectedPermissionKey"
              :options="permissionOptions"
              placeholder="Select permission to assign"
              searchable
              clearable
              size="sm"
              class="flex-1 max-w-md"
            />
            <Button
              variant="outline"
              size="sm"
              :loading="permissionAssigning"
              :disabled="!selectedPermissionKey"
              @click="handleAssignPermission"
            >
              Assign
            </Button>
          </div>
        </section>

        <!-- Info Section -->
        <section>
          <h3 class="text-sm font-semibold text-text-heading mb-2">Info</h3>
          <dl class="grid grid-cols-2 gap-x-4 gap-y-1 text-sm">
            <dt class="text-text-meta">Service Scope</dt>
            <dd class="text-text-body">{{ selectedRole.serviceScope ?? '-' }}</dd>
            <dt class="text-text-meta">Membership Group</dt>
            <dd class="text-text-body">{{ selectedRole.membershipGroup ?? '-' }}</dd>
            <dt class="text-text-meta">Parent Role</dt>
            <dd class="text-text-body">{{ selectedRole.parentRoleKey ?? '-' }}</dd>
            <dt class="text-text-meta">Created</dt>
            <dd class="text-text-body">{{ selectedRole.createdAt ?? '-' }}</dd>
            <dt class="text-text-meta">Updated</dt>
            <dd class="text-text-body">{{ selectedRole.updatedAt ?? '-' }}</dd>
          </dl>
        </section>
      </template>
    </Card>
  </div>
</template>
