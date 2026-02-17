<script setup lang="ts">
import { ref, watch } from 'vue';
import { Tag, Spinner } from '@portal/design-vue';
import { fetchRoleDefaults } from '@/api/admin';
import type { RoleDefaultMappingResponse } from '@/dto/admin';

const props = defineProps<{
  roleKey: string;
}>();

const mappings = ref<RoleDefaultMappingResponse[]>([]);
const loading = ref(false);

async function load(roleKey: string) {
  loading.value = true;
  try {
    mappings.value = await fetchRoleDefaults(roleKey);
  } catch {
    mappings.value = [];
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
    <h3 class="text-sm font-semibold text-text-heading mb-3">Default Memberships</h3>

    <div v-if="loading" class="flex justify-center py-2">
      <Spinner size="sm" />
    </div>

    <div v-else-if="mappings.length === 0" class="text-sm text-text-meta">
      No default membership mappings
    </div>

    <div v-else class="flex flex-wrap gap-2">
      <Tag
        v-for="m in mappings"
        :key="m.id"
        variant="default"
        size="md"
      >
        <span class="font-mono text-xs">{{ m.membershipGroup }}</span>
        <span class="mx-1 text-text-muted">&rarr;</span>
        <span class="font-mono text-xs font-semibold">{{ m.defaultTierKey }}</span>
      </Tag>
    </div>
  </div>
</template>
