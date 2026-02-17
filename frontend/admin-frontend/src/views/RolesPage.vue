<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { Badge, Spinner, Alert, Select, Input, Button, Card, Tag, Tooltip, useApiError } from '@portal/design-vue';
import type { SelectOption } from '@portal/design-vue';
import {
  fetchRoles,
  fetchRoleDetail,
  createRole,
  updateRole,
  toggleRoleActive,
  fetchPermissions,
  assignPermission,
  removePermission,
  addRoleInclude,
  removeRoleInclude,
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
  includedRoleKeys: [],
});
const createSaving = ref(false);
const createError = ref('');

// Permission assignment
const selectedPermissionKey = ref<string | null>(null);
const permissionAssigning = ref(false);

// Include management
const selectedIncludeRoleKey = ref<string | null>(null);
const includeAdding = ref(false);

// === Computed ===
const filteredRoles = computed(() => {
  if (!searchQuery.value) return roles.value;
  const q = searchQuery.value.toLowerCase();
  return roles.value.filter(
    (r) => r.roleKey.toLowerCase().includes(q) || r.displayName.toLowerCase().includes(q),
  );
});

const includeRoleOptions = computed<SelectOption[]>(() => {
  const currentIncludes = new Set(selectedRole.value?.includedRoleKeys ?? []);
  const currentKey = selectedRole.value?.roleKey;
  return roles.value
    .filter((r) => r.active && r.roleKey !== currentKey && !currentIncludes.has(r.roleKey))
    .map((r) => ({ value: r.roleKey, label: `${r.displayName} (${r.roleKey})` }));
});

const createIncludeOptions = computed<SelectOption[]>(() =>
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
    includedRoleKeys: [],
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
    if (createForm.value.includedRoleKeys?.length) payload.includedRoleKeys = createForm.value.includedRoleKeys;

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

async function handleAddInclude() {
  if (!selectedRole.value || !selectedIncludeRoleKey.value) return;
  includeAdding.value = true;
  try {
    await addRoleInclude(selectedRole.value.roleKey, selectedIncludeRoleKey.value);
    selectedIncludeRoleKey.value = null;
    await selectRole(selectedRole.value.roleKey);
    await loadRoles();
  } catch (e: unknown) {
    error.value = getErrorMessage(e, 'Failed to add include');
  } finally {
    includeAdding.value = false;
  }
}

async function handleRemoveInclude(includedRoleKey: string) {
  if (!selectedRole.value) return;
  try {
    await removeRoleInclude(selectedRole.value.roleKey, includedRoleKey);
    await selectRole(selectedRole.value.roleKey);
    await loadRoles();
  } catch (e: unknown) {
    error.value = getErrorMessage(e, 'Failed to remove include');
  }
}

onMounted(() => {
  loadRoles();
  loadPermissions();
});
</script>

<template>
  <div>
    <!-- Header -->
    <div class="flex items-center justify-between mb-6">
      <h1 class="text-2xl font-bold text-text-heading">Roles</h1>
      <Button variant="primary" size="sm" @click="enterCreateMode">
        <span class="material-symbols-outlined" style="font-size: 18px;">add</span>
        Create Role
      </Button>
    </div>

    <!-- Error -->
    <Alert v-if="error" variant="error" dismissible class="mb-4" @dismiss="error = ''">
      {{ error }}
    </Alert>

    <!-- Loading -->
    <div v-if="loading" class="flex justify-center py-12">
      <Spinner size="lg" />
    </div>

    <div v-else class="flex gap-6">
      <!-- Role List (Left Panel) -->
      <div class="w-80 shrink-0">
        <!-- Search -->
        <div class="relative mb-3">
          <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-text-muted" style="font-size: 18px;">search</span>
          <input
            v-model="searchQuery"
            type="text"
            placeholder="Search roles..."
            class="w-full pl-9 pr-3 py-2 bg-bg-card border border-border-default rounded-lg text-sm text-text-body placeholder:text-text-muted focus:outline-none focus:border-border-focus"
          />
        </div>

        <Card variant="elevated" padding="none" class="overflow-hidden">
          <div class="max-h-[calc(100vh-280px)] overflow-y-auto">
            <div v-if="filteredRoles.length === 0" class="px-4 py-8 text-center text-text-meta text-sm">
              No roles found
            </div>
            <div
              v-for="role in filteredRoles"
              :key="role.id"
              @click="selectRole(role.roleKey)"
              :class="[
                'px-4 py-3 cursor-pointer transition-colors border-b border-border-muted last:border-b-0',
                selectedRole?.roleKey === role.roleKey
                  ? 'bg-brand-primary/5 border-l-2 border-l-brand-primary'
                  : 'hover:bg-bg-hover border-l-2 border-l-transparent'
              ]"
            >
              <div class="flex items-center justify-between">
                <span class="text-sm font-medium text-text-heading">{{ role.displayName }}</span>
                <Badge :variant="role.active ? 'success' : 'danger'" size="sm">
                  {{ role.active ? 'Active' : 'Inactive' }}
                </Badge>
              </div>
              <div class="text-xs text-text-meta font-mono mt-0.5">{{ role.roleKey }}</div>
              <div class="flex items-center gap-2 mt-1">
                <Badge v-if="role.system" variant="info" size="sm">System</Badge>
                <span v-if="role.serviceScope" class="text-[11px] text-text-muted">{{ role.serviceScope }}</span>
              </div>
            </div>
          </div>
        </Card>
      </div>

      <!-- Right Panel -->
      <div class="flex-1 min-w-0">
        <!-- Create Mode -->
        <Card v-if="createMode" variant="elevated" padding="lg">
          <h2 class="text-base font-semibold text-text-heading mb-4">Create New Role</h2>

          <Alert v-if="createError" variant="error" dismissible class="mb-4" @dismiss="createError = ''">
            {{ createError }}
          </Alert>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
            <Input v-model="createForm.roleKey" label="Role Key" placeholder="ROLE_CUSTOM_NAME" required />
            <Input v-model="createForm.displayName" label="Display Name" placeholder="Custom Role Name" required />
            <Input v-model="createForm.description" label="Description" placeholder="Role description" />
            <Input v-model="createForm.serviceScope" label="Service Scope" placeholder="e.g. shopping" />
            <Input v-model="createForm.membershipGroup" label="Membership Group" placeholder="e.g. USER_SHOPPING" />
            <Select
              v-model="createForm.includedRoleKeys"
              :options="createIncludeOptions"
              label="Included Roles"
              placeholder="Select included roles (optional)"
              clearable
              searchable
              multiple
            />
          </div>

          <div class="flex gap-2">
            <Button variant="primary" size="sm" :loading="createSaving" @click="handleCreate">
              Create
            </Button>
            <Button variant="ghost" size="sm" @click="createMode = false">
              Cancel
            </Button>
          </div>
        </Card>

        <!-- Role Detail -->
        <template v-else-if="selectedRole || detailLoading">
          <Card variant="elevated" padding="none">
            <div v-if="detailLoading" class="flex justify-center py-12">
              <Spinner size="md" />
            </div>

            <template v-else-if="selectedRole">
              <!-- Header -->
              <div class="p-5 border-b border-border-default">
                <div class="flex items-start justify-between">
                  <div>
                    <h2 class="text-lg font-semibold text-text-heading">{{ selectedRole.displayName }}</h2>
                    <p class="font-mono text-xs text-text-meta mt-0.5">{{ selectedRole.roleKey }}</p>
                    <p v-if="selectedRole.description && !editMode" class="text-sm text-text-body mt-2">
                      {{ selectedRole.description }}
                    </p>
                  </div>
                  <div class="flex items-center gap-2">
                    <Badge v-if="selectedRole.system" variant="info" size="sm">System</Badge>
                    <Badge :variant="selectedRole.active ? 'success' : 'danger'" size="sm">
                      {{ selectedRole.active ? 'Active' : 'Inactive' }}
                    </Badge>
                  </div>
                </div>

                <!-- Edit Mode -->
                <div v-if="editMode" class="mt-4 space-y-3">
                  <Input v-model="editDisplayName" label="Display Name" required />
                  <Input v-model="editDescription" label="Description" />
                  <div class="flex gap-2">
                    <Button variant="primary" size="sm" :loading="editSaving" @click="saveEdit">
                      <span class="material-symbols-outlined" style="font-size: 16px;">save</span>
                      Save Changes
                    </Button>
                    <Button variant="ghost" size="sm" @click="editMode = false">
                      Cancel
                    </Button>
                  </div>
                </div>

                <!-- Actions -->
                <div v-else class="flex gap-2 mt-3">
                  <Tooltip content="Edit role details">
                    <Button variant="outline" size="sm" @click="enterEditMode">
                      <span class="material-symbols-outlined" style="font-size: 16px;">edit</span>
                      Edit
                    </Button>
                  </Tooltip>
                  <Button
                    v-if="!selectedRole.system"
                    :variant="selectedRole.active ? 'danger' : 'secondary'"
                    size="sm"
                    @click="handleToggleActive"
                  >
                    <span class="material-symbols-outlined" style="font-size: 16px;">
                      {{ selectedRole.active ? 'block' : 'check_circle' }}
                    </span>
                    {{ selectedRole.active ? 'Deactivate' : 'Activate' }}
                  </Button>
                </div>
              </div>

              <!-- Own Permissions -->
              <div class="p-5 border-b border-border-default">
                <h3 class="text-sm font-semibold text-text-heading mb-3">Own Permissions</h3>
                <div class="flex flex-wrap gap-2 mb-3">
                  <Tag
                    v-for="perm in selectedRole.permissions"
                    :key="perm.id"
                    variant="default"
                    size="md"
                    removable
                    @remove="handleRemovePermission(perm.permissionKey)"
                  >
                    <Tooltip v-if="perm.description" :content="perm.description">
                      <span class="font-mono">{{ perm.permissionKey }}</span>
                    </Tooltip>
                    <span v-else class="font-mono">{{ perm.permissionKey }}</span>
                  </Tag>
                  <p v-if="selectedRole.permissions.length === 0" class="text-sm text-text-meta">
                    No permissions assigned
                  </p>
                </div>

                <div class="flex items-end gap-2">
                  <Select
                    v-model="selectedPermissionKey"
                    :options="permissionOptions"
                    placeholder="Add permission..."
                    searchable
                    clearable
                    size="sm"
                    class="flex-1"
                  />
                  <Button
                    variant="primary"
                    size="sm"
                    :disabled="!selectedPermissionKey"
                    :loading="permissionAssigning"
                    @click="handleAssignPermission"
                  >
                    Add
                  </Button>
                </div>
              </div>

              <!-- Included Roles -->
              <div class="p-5 border-b border-border-default">
                <h3 class="text-sm font-semibold text-text-heading mb-3">Included Roles</h3>
                <div class="flex flex-wrap gap-2 mb-3">
                  <Tag
                    v-for="key in selectedRole.includedRoleKeys"
                    :key="key"
                    variant="default"
                    size="md"
                    removable
                    @remove="handleRemoveInclude(key)"
                  >
                    <span class="font-mono">{{ key }}</span>
                  </Tag>
                  <p v-if="selectedRole.includedRoleKeys.length === 0" class="text-sm text-text-meta">
                    No included roles
                  </p>
                </div>

                <div class="flex items-end gap-2">
                  <Select
                    v-model="selectedIncludeRoleKey"
                    :options="includeRoleOptions"
                    placeholder="Add included role..."
                    searchable
                    clearable
                    size="sm"
                    class="flex-1"
                  />
                  <Button
                    variant="primary"
                    size="sm"
                    :disabled="!selectedIncludeRoleKey"
                    :loading="includeAdding"
                    @click="handleAddInclude"
                  >
                    Add
                  </Button>
                </div>
              </div>

              <!-- Effective Roles -->
              <div class="p-5 border-b border-border-default">
                <h3 class="text-sm font-semibold text-text-heading mb-3">Effective Roles</h3>
                <div class="flex flex-wrap gap-2">
                  <Tag
                    v-for="key in selectedRole.effectiveRoleKeys"
                    :key="key"
                    variant="info"
                    size="sm"
                  >
                    <span class="font-mono">{{ key }}</span>
                  </Tag>
                  <p v-if="selectedRole.effectiveRoleKeys.length === 0" class="text-sm text-text-meta">
                    No effective roles
                  </p>
                </div>
              </div>

              <!-- Role Info -->
              <div class="p-5">
                <h3 class="text-sm font-semibold text-text-heading mb-3">Info</h3>
                <dl class="grid grid-cols-2 gap-x-6 gap-y-2 text-sm">
                  <dt class="text-text-meta">Service Scope</dt>
                  <dd class="text-text-body">{{ selectedRole.serviceScope ?? '-' }}</dd>
                  <dt class="text-text-meta">Membership Group</dt>
                  <dd class="text-text-body">{{ selectedRole.membershipGroup ?? '-' }}</dd>
                  <dt class="text-text-meta">Created</dt>
                  <dd class="text-text-body">{{ selectedRole.createdAt ?? '-' }}</dd>
                  <dt class="text-text-meta">Updated</dt>
                  <dd class="text-text-body">{{ selectedRole.updatedAt ?? '-' }}</dd>
                </dl>
              </div>
            </template>
          </Card>
        </template>

        <!-- Empty State -->
        <div v-else class="flex flex-col items-center justify-center py-20 text-text-muted">
          <span class="material-symbols-outlined mb-3" style="font-size: 48px; opacity: 0.3;">shield</span>
          <p class="text-sm">Select a role to view details</p>
        </div>
      </div>
    </div>
  </div>
</template>
