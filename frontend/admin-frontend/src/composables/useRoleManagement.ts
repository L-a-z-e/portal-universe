import { ref, computed } from 'vue';
import { useApiError } from '@portal/design-vue';
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

export function useRoleManagement() {
  const { getErrorMessage } = useApiError();

  // === State ===
  const roles = ref<RoleResponse[]>([]);
  const allPermissions = ref<PermissionResponse[]>([]);
  const loading = ref(true);
  const error = ref('');
  const searchQuery = ref('');

  const selectedRole = ref<RoleDetailResponse | null>(null);
  const detailLoading = ref(false);

  const editMode = ref(false);
  const editDisplayName = ref('');
  const editDescription = ref('');
  const editSaving = ref(false);

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

  const selectedPermissionKey = ref<string | null>(null);
  const permissionAssigning = ref(false);

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

  return {
    // State
    roles,
    allPermissions,
    loading,
    error,
    searchQuery,
    selectedRole,
    detailLoading,
    editMode,
    editDisplayName,
    editDescription,
    editSaving,
    createMode,
    createForm,
    createSaving,
    createError,
    selectedPermissionKey,
    permissionAssigning,
    selectedIncludeRoleKey,
    includeAdding,
    // Computed
    filteredRoles,
    includeRoleOptions,
    createIncludeOptions,
    permissionOptions,
    // Actions
    loadRoles,
    loadPermissions,
    selectRole,
    enterEditMode,
    saveEdit,
    handleToggleActive,
    enterCreateMode,
    handleCreate,
    handleAssignPermission,
    handleRemovePermission,
    handleAddInclude,
    handleRemoveInclude,
  };
}
