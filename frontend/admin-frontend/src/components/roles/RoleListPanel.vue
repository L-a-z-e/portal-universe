<script setup lang="ts">
import { Badge, Card } from '@portal/design-vue';
import type { RoleResponse, RoleDetailResponse } from '@/dto/admin';

defineProps<{
  roles: RoleResponse[];
  selectedRole: RoleDetailResponse | null;
  searchQuery: string;
}>();

const emit = defineEmits<{
  selectRole: [roleKey: string];
  'update:searchQuery': [value: string];
}>();
</script>

<template>
  <div class="w-80 shrink-0">
    <div class="relative mb-3">
      <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-text-muted" style="font-size: 18px;">search</span>
      <input
        :value="searchQuery"
        @input="emit('update:searchQuery', ($event.target as HTMLInputElement).value)"
        type="text"
        placeholder="Search roles..."
        class="w-full pl-9 pr-3 py-2 bg-bg-card border border-border-default rounded-lg text-sm text-text-body placeholder:text-text-muted focus:outline-none focus:border-border-focus"
      />
    </div>

    <Card variant="elevated" padding="none" class="overflow-hidden">
      <div class="max-h-[calc(100vh-280px)] overflow-y-auto">
        <div v-if="roles.length === 0" class="px-4 py-8 text-center text-text-meta text-sm">
          No roles found
        </div>
        <div
          v-for="role in roles"
          :key="role.id"
          @click="emit('selectRole', role.roleKey)"
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
</template>
