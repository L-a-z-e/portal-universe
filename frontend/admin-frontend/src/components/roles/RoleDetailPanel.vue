<script setup lang="ts">
import { Badge, Spinner, Select, Input, Button, Card, Tag, Tooltip } from '@portal/design-vue';
import type { SelectOption } from '@portal/design-vue';
import type { RoleDetailResponse } from '@/dto/admin';

defineProps<{
  selectedRole: RoleDetailResponse | null;
  detailLoading: boolean;
  editMode: boolean;
  editDisplayName: string;
  editDescription: string;
  editSaving: boolean;
  selectedPermissionKey: string | null;
  permissionAssigning: boolean;
  permissionOptions: SelectOption[];
  selectedIncludeRoleKey: string | null;
  includeAdding: boolean;
  includeRoleOptions: SelectOption[];
}>();

const emit = defineEmits<{
  enterEditMode: [];
  saveEdit: [];
  cancelEdit: [];
  toggleActive: [];
  assignPermission: [];
  removePermission: [permissionKey: string];
  addInclude: [];
  removeInclude: [roleKey: string];
  'update:editDisplayName': [value: string];
  'update:editDescription': [value: string];
  'update:selectedPermissionKey': [value: string | null];
  'update:selectedIncludeRoleKey': [value: string | null];
}>();

function onEditDisplayName(val: string | number) {
  emit('update:editDisplayName', String(val));
}
function onEditDescription(val: string | number) {
  emit('update:editDescription', String(val));
}
function onPermissionKeyChange(val: string | number | null) {
  emit('update:selectedPermissionKey', val == null ? null : String(val));
}
function onIncludeRoleKeyChange(val: string | number | null) {
  emit('update:selectedIncludeRoleKey', val == null ? null : String(val));
}
</script>

<template>
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
          <Input :modelValue="editDisplayName" @update:modelValue="onEditDisplayName" label="Display Name" required />
          <Input :modelValue="editDescription" @update:modelValue="onEditDescription" label="Description" />
          <div class="flex gap-2">
            <Button variant="primary" size="sm" :loading="editSaving" @click="emit('saveEdit')">
              <span class="material-symbols-outlined" style="font-size: 16px;">save</span>
              Save Changes
            </Button>
            <Button variant="ghost" size="sm" @click="emit('cancelEdit')">
              Cancel
            </Button>
          </div>
        </div>

        <!-- Actions -->
        <div v-else class="flex gap-2 mt-3">
          <Tooltip content="Edit role details">
            <Button variant="outline" size="sm" @click="emit('enterEditMode')">
              <span class="material-symbols-outlined" style="font-size: 16px;">edit</span>
              Edit
            </Button>
          </Tooltip>
          <Button
            v-if="!selectedRole.system"
            :variant="selectedRole.active ? 'danger' : 'secondary'"
            size="sm"
            @click="emit('toggleActive')"
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
            @remove="emit('removePermission', perm.permissionKey)"
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
            :modelValue="selectedPermissionKey"
            @update:modelValue="onPermissionKeyChange"
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
            @click="emit('assignPermission')"
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
            @remove="emit('removeInclude', key)"
          >
            <span class="font-mono">{{ key }}</span>
          </Tag>
          <p v-if="selectedRole.includedRoleKeys.length === 0" class="text-sm text-text-meta">
            No included roles
          </p>
        </div>

        <div class="flex items-end gap-2">
          <Select
            :modelValue="selectedIncludeRoleKey"
            @update:modelValue="onIncludeRoleKeyChange"
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
            @click="emit('addInclude')"
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

      <!-- Slot for additional sections (resolved permissions, role-default mappings) -->
      <slot name="extra-sections" />

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
