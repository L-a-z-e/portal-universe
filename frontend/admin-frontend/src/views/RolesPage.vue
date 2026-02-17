<script setup lang="ts">
import { onMounted } from 'vue';
import { Spinner, Alert, Button } from '@portal/design-vue';
import { useRoleManagement } from '@/composables/useRoleManagement';
import RoleListPanel from '@/components/roles/RoleListPanel.vue';
import RoleDetailPanel from '@/components/roles/RoleDetailPanel.vue';
import RoleCreateForm from '@/components/roles/RoleCreateForm.vue';
import RoleDagView from '@/components/roles/RoleDagView.vue';
import ResolvedPermissionsTable from '@/components/roles/ResolvedPermissionsTable.vue';
import RoleDefaultMappings from '@/components/roles/RoleDefaultMappings.vue';

const rm = useRoleManagement();

function handleDagSelectRole(roleKey: string) {
  rm.selectRole(roleKey);
}

onMounted(() => {
  rm.loadRoles();
  rm.loadPermissions();
});
</script>

<template>
  <div>
    <!-- Header -->
    <div class="flex items-center justify-between mb-6">
      <h1 class="text-2xl font-bold text-text-heading">Roles</h1>
      <Button variant="primary" size="sm" @click="rm.enterCreateMode()">
        <span class="material-symbols-outlined" style="font-size: 18px;">add</span>
        Create Role
      </Button>
    </div>

    <!-- Error -->
    <Alert v-if="rm.error.value" variant="error" dismissible class="mb-4" @dismiss="rm.error.value = ''">
      {{ rm.error.value }}
    </Alert>

    <!-- Loading -->
    <div v-if="rm.loading.value" class="flex justify-center py-12">
      <Spinner size="lg" />
    </div>

    <div v-else class="flex gap-6">
      <!-- Left Panel: Role List -->
      <RoleListPanel
        :roles="rm.filteredRoles.value"
        :selectedRole="rm.selectedRole.value"
        :searchQuery="rm.searchQuery.value"
        @update:searchQuery="rm.searchQuery.value = $event"
        @selectRole="rm.selectRole($event)"
      />

      <!-- Right Panel -->
      <div class="flex-1 min-w-0 space-y-4">
        <!-- Create Mode -->
        <RoleCreateForm
          v-if="rm.createMode.value"
          :createForm="rm.createForm.value"
          :createSaving="rm.createSaving.value"
          :createError="rm.createError.value"
          :createIncludeOptions="rm.createIncludeOptions.value"
          @update:createForm="rm.createForm.value = $event"
          @update:createError="rm.createError.value = $event"
          @create="rm.handleCreate()"
          @cancel="rm.createMode.value = false"
        />

        <!-- Role Detail -->
        <template v-else-if="rm.selectedRole.value || rm.detailLoading.value">
          <!-- DAG Visualization -->
          <RoleDagView
            :selectedRoleKey="rm.selectedRole.value?.roleKey"
            @selectRole="handleDagSelectRole"
          />

          <RoleDetailPanel
            :selectedRole="rm.selectedRole.value"
            :detailLoading="rm.detailLoading.value"
            :editMode="rm.editMode.value"
            :editDisplayName="rm.editDisplayName.value"
            :editDescription="rm.editDescription.value"
            :editSaving="rm.editSaving.value"
            :selectedPermissionKey="rm.selectedPermissionKey.value"
            :permissionAssigning="rm.permissionAssigning.value"
            :permissionOptions="rm.permissionOptions.value"
            :selectedIncludeRoleKey="rm.selectedIncludeRoleKey.value"
            :includeAdding="rm.includeAdding.value"
            :includeRoleOptions="rm.includeRoleOptions.value"
            @enterEditMode="rm.enterEditMode()"
            @saveEdit="rm.saveEdit()"
            @cancelEdit="rm.editMode.value = false"
            @toggleActive="rm.handleToggleActive()"
            @assignPermission="rm.handleAssignPermission()"
            @removePermission="rm.handleRemovePermission($event)"
            @addInclude="rm.handleAddInclude()"
            @removeInclude="rm.handleRemoveInclude($event)"
            @update:editDisplayName="rm.editDisplayName.value = $event"
            @update:editDescription="rm.editDescription.value = $event"
            @update:selectedPermissionKey="rm.selectedPermissionKey.value = $event"
            @update:selectedIncludeRoleKey="rm.selectedIncludeRoleKey.value = $event"
          >
            <template #extra-sections>
              <ResolvedPermissionsTable
                v-if="rm.selectedRole.value"
                :roleKey="rm.selectedRole.value.roleKey"
                :ownPermissions="rm.selectedRole.value.permissions"
              />
              <RoleDefaultMappings
                v-if="rm.selectedRole.value"
                :roleKey="rm.selectedRole.value.roleKey"
              />
            </template>
          </RoleDetailPanel>
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
