<script setup lang="ts">
import { ref, watch, computed } from 'vue';
import { Badge, Spinner, Table } from '@portal/design-vue';
import type { TableColumn } from '@portal/design-core';
import { fetchResolvedRole } from '@/api/admin';
import type { ResolvedRoleResponse, PermissionResponse } from '@/dto/admin';

const props = defineProps<{
  roleKey: string;
  ownPermissions: PermissionResponse[];
}>();

const resolved = ref<ResolvedRoleResponse | null>(null);
const loading = ref(false);

interface ResolvedPermission {
  key: string;
  source: 'own' | 'inherited';
}

const resolvedPermissions = computed<ResolvedPermission[]>(() => {
  if (!resolved.value) return [];
  const ownKeys = new Set(props.ownPermissions.map((p) => p.permissionKey));
  return resolved.value.effectivePermissions.map((key) => ({
    key,
    source: ownKeys.has(key) ? 'own' as const : 'inherited' as const,
  }));
});

const permissionColumns: TableColumn<ResolvedPermission>[] = [
  { key: 'key', label: 'Permission' },
  { key: 'source', label: 'Source', width: '96px' },
];

async function load(roleKey: string) {
  loading.value = true;
  try {
    resolved.value = await fetchResolvedRole(roleKey);
  } catch {
    resolved.value = null;
  } finally {
    loading.value = false;
  }
}

watch(() => props.roleKey, (key) => {
  if (key) load(key);
}, { immediate: true });
</script>

<template>
  <div class="p-5 border-b border-border-default">
    <h3 class="text-sm font-semibold text-text-heading mb-3">Resolved Permissions</h3>

    <div v-if="loading" class="flex justify-center py-4">
      <Spinner size="sm" />
    </div>

    <div v-else-if="resolvedPermissions.length === 0" class="text-sm text-text-meta">
      No resolved permissions
    </div>

    <div v-else class="max-h-60 overflow-y-auto">
      <Table :columns="permissionColumns" :data="resolvedPermissions" :hoverable="false">
        <template #cell-key="{ value }">
          <span class="font-mono">{{ value }}</span>
        </template>
        <template #cell-source="{ row }">
          <Badge
            :variant="row.source === 'own' ? 'success' : 'info'"
            size="sm"
          >
            {{ row.source === 'own' ? 'Own' : 'Inherited' }}
          </Badge>
        </template>
      </Table>
    </div>
  </div>
</template>
