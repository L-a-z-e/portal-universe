<script setup lang="ts">
import { ref, watch, computed } from 'vue';
import { Badge, Spinner } from '@portal/design-vue';
import { fetchResolvedRole } from '@/api/admin';
import type { ResolvedRoleResponse, PermissionResponse } from '@/dto/admin';

const props = defineProps<{
  roleKey: string;
  ownPermissions: PermissionResponse[];
}>();

const resolved = ref<ResolvedRoleResponse | null>(null);
const loading = ref(false);

const resolvedPermissions = computed(() => {
  if (!resolved.value) return [];
  const ownKeys = new Set(props.ownPermissions.map((p) => p.permissionKey));
  return resolved.value.effectivePermissions.map((key) => ({
    key,
    source: ownKeys.has(key) ? 'own' as const : 'inherited' as const,
  }));
});

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
      <table class="w-full text-sm">
        <thead class="sticky top-0 bg-bg-card">
          <tr class="border-b border-border-default">
            <th class="text-left py-1.5 px-2 text-text-meta font-medium">Permission</th>
            <th class="text-left py-1.5 px-2 text-text-meta font-medium w-24">Source</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="perm in resolvedPermissions"
            :key="perm.key"
            class="border-b border-border-muted last:border-b-0"
          >
            <td class="py-1.5 px-2 font-mono text-text-body">{{ perm.key }}</td>
            <td class="py-1.5 px-2">
              <Badge
                :variant="perm.source === 'own' ? 'success' : 'info'"
                size="sm"
              >
                {{ perm.source === 'own' ? 'Own' : 'Inherited' }}
              </Badge>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
