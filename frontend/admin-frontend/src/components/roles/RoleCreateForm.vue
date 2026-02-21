<script setup lang="ts">
import { Alert, Select, Input, Button, Card } from '@portal/design-vue';
import type { SelectOption } from '@portal/design-vue';
import type { CreateRoleRequest } from '@/dto/admin';

const props = defineProps<{
  createForm: CreateRoleRequest;
  createSaving: boolean;
  createError: string;
  createIncludeOptions: SelectOption[];
}>();

const emit = defineEmits<{
  create: [];
  cancel: [];
  'update:createForm': [value: CreateRoleRequest];
  'update:createError': [value: string];
}>();

function updateField<K extends keyof CreateRoleRequest>(field: K, value: CreateRoleRequest[K]) {
  emit('update:createForm', { ...props.createForm, [field]: value });
}
</script>

<template>
  <Card variant="elevated" padding="lg">
    <h2 class="text-base font-semibold text-text-heading mb-4">Create New Role</h2>

    <Alert v-if="createError" variant="error" dismissible class="mb-4" @dismiss="emit('update:createError', '')">
      {{ createError }}
    </Alert>

    <div class="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
      <Input :modelValue="createForm.roleKey" @update:modelValue="updateField('roleKey', $event as string)" label="Role Key" placeholder="ROLE_CUSTOM_NAME" required />
      <Input :modelValue="createForm.displayName" @update:modelValue="updateField('displayName', $event as string)" label="Display Name" placeholder="Custom Role Name" required />
      <Input :modelValue="createForm.description" @update:modelValue="updateField('description', $event as string)" label="Description" placeholder="Role description" />
      <Input :modelValue="createForm.serviceScope" @update:modelValue="updateField('serviceScope', $event as string)" label="Service Scope" placeholder="e.g. shopping" />
      <Input :modelValue="createForm.membershipGroup" @update:modelValue="updateField('membershipGroup', $event as string)" label="Membership Group" placeholder="e.g. USER_SHOPPING" />
      <Select
        :modelValue="(createForm.includedRoleKeys as unknown as string | number | undefined)"
        @update:modelValue="updateField('includedRoleKeys', ($event as unknown) as string[])"
        :options="createIncludeOptions"
        label="Included Roles"
        placeholder="Select included roles (optional)"
        clearable
        searchable
        multiple
      />
    </div>

    <div class="flex gap-2">
      <Button variant="primary" size="sm" :loading="createSaving" @click="emit('create')">
        Create
      </Button>
      <Button variant="ghost" size="sm" @click="emit('cancel')">
        Cancel
      </Button>
    </div>
  </Card>
</template>
