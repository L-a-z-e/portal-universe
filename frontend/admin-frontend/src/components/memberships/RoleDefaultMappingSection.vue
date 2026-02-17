<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { Badge, Select, Button, Card, Spinner, Alert, useApiError, useToast } from '@portal/design-vue';
import type { SelectOption } from '@portal/design-vue';
import {
  fetchAllRoleDefaults,
  addRoleDefault,
  removeRoleDefault,
  fetchRoles,
  fetchMembershipGroups,
  fetchMembershipTiers,
} from '@/api/admin';
import type { RoleDefaultMappingResponse, RoleResponse, MembershipTierResponse } from '@/dto/admin';

const { getErrorMessage } = useApiError();
const { success: toastSuccess } = useToast();

const mappings = ref<RoleDefaultMappingResponse[]>([]);
const roles = ref<RoleResponse[]>([]);
const groups = ref<string[]>([]);
const groupTiers = ref<Record<string, MembershipTierResponse[]>>({});
const loading = ref(true);
const error = ref('');
const adding = ref(false);

// Add form
const newRoleKey = ref<string | null>(null);
const newGroup = ref<string | null>(null);
const newTierKey = ref<string | null>(null);

const roleOptions = computed<SelectOption[]>(() =>
  roles.value
    .filter((r) => r.active)
    .map((r) => ({ value: r.roleKey, label: `${r.displayName} (${r.roleKey})` })),
);

const groupOptions = computed<SelectOption[]>(() =>
  groups.value.map((g) => ({ value: g, label: g })),
);

const tierOptions = computed<SelectOption[]>(() => {
  if (!newGroup.value) return [];
  return (groupTiers.value[newGroup.value] ?? []).map((t) => ({
    value: t.tierKey,
    label: `${t.displayName} (${t.tierKey})`,
  }));
});

const canAdd = computed(() => newRoleKey.value && newGroup.value && newTierKey.value);

async function loadData() {
  loading.value = true;
  error.value = '';
  try {
    const [m, r, g] = await Promise.all([
      fetchAllRoleDefaults(),
      fetchRoles(),
      fetchMembershipGroups(),
    ]);
    mappings.value = m;
    roles.value = r;
    groups.value = g;

    // Load tiers for all groups
    const tierResults = await Promise.all(g.map((group) => fetchMembershipTiers(group)));
    g.forEach((group, i) => {
      groupTiers.value[group] = tierResults[i]!;
    });
  } catch (e: unknown) {
    error.value = getErrorMessage(e, 'Failed to load data');
  } finally {
    loading.value = false;
  }
}

async function handleAdd() {
  if (!newRoleKey.value || !newGroup.value || !newTierKey.value) return;
  adding.value = true;
  error.value = '';
  try {
    const created = await addRoleDefault({
      roleKey: newRoleKey.value,
      membershipGroup: newGroup.value,
      defaultTierKey: newTierKey.value,
    });
    mappings.value.push(created);
    newRoleKey.value = null;
    newGroup.value = null;
    newTierKey.value = null;
    toastSuccess('Role-default mapping added');
  } catch (e: unknown) {
    error.value = getErrorMessage(e, 'Failed to add mapping');
  } finally {
    adding.value = false;
  }
}

async function handleRemove(mapping: RoleDefaultMappingResponse) {
  error.value = '';
  try {
    await removeRoleDefault(mapping.roleKey, mapping.membershipGroup);
    mappings.value = mappings.value.filter((m) => m.id !== mapping.id);
    toastSuccess('Role-default mapping removed');
  } catch (e: unknown) {
    error.value = getErrorMessage(e, 'Failed to remove mapping');
  }
}

onMounted(loadData);
</script>

<template>
  <Card variant="elevated" padding="lg">
    <h2 class="text-base font-semibold text-text-heading mb-4">Role-Default Membership Mappings</h2>

    <Alert v-if="error" variant="error" dismissible class="mb-4" @dismiss="error = ''">
      {{ error }}
    </Alert>

    <div v-if="loading" class="flex justify-center py-4">
      <Spinner size="md" />
    </div>

    <template v-else>
      <!-- Table -->
      <div class="overflow-x-auto mb-4">
        <table class="w-full text-sm">
          <thead>
            <tr class="border-b border-border-default">
              <th class="text-left py-2 px-3 text-text-meta font-medium">Role Key</th>
              <th class="text-left py-2 px-3 text-text-meta font-medium">Membership Group</th>
              <th class="text-left py-2 px-3 text-text-meta font-medium">Default Tier</th>
              <th class="text-right py-2 px-3 text-text-meta font-medium w-20">Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="mappings.length === 0">
              <td colspan="4" class="py-4 text-center text-text-meta">No mappings found</td>
            </tr>
            <tr
              v-for="m in mappings"
              :key="m.id"
              class="border-b border-border-muted last:border-b-0"
            >
              <td class="py-2 px-3 font-mono text-text-body">{{ m.roleKey }}</td>
              <td class="py-2 px-3">
                <Badge variant="info" size="sm">{{ m.membershipGroup }}</Badge>
              </td>
              <td class="py-2 px-3 font-mono font-semibold text-text-body">{{ m.defaultTierKey }}</td>
              <td class="py-2 px-3 text-right">
                <Button variant="ghost" size="sm" @click="handleRemove(m)">
                  <span class="material-symbols-outlined text-status-error" style="font-size: 16px;">delete</span>
                </Button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Add Form -->
      <div class="border-t border-border-default pt-4">
        <h3 class="text-sm font-medium text-text-heading mb-3">Add Mapping</h3>
        <div class="flex items-end gap-3">
          <Select
            v-model="newRoleKey"
            :options="roleOptions"
            placeholder="Select role..."
            searchable
            clearable
            size="sm"
            class="flex-1"
            label="Role"
          />
          <Select
            v-model="newGroup"
            :options="groupOptions"
            placeholder="Select group..."
            searchable
            clearable
            size="sm"
            class="flex-1"
            label="Group"
          />
          <Select
            v-model="newTierKey"
            :options="tierOptions"
            placeholder="Select tier..."
            searchable
            clearable
            size="sm"
            class="flex-1"
            label="Tier"
            :disabled="!newGroup"
          />
          <Button
            variant="primary"
            size="sm"
            :disabled="!canAdd"
            :loading="adding"
            @click="handleAdd"
          >
            Add
          </Button>
        </div>
      </div>
    </template>
  </Card>
</template>
