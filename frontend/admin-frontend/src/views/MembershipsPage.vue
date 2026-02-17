<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { Badge, Spinner, Alert, Input, Button, Card, useApiError, useToast } from '@portal/design-vue';
import {
  fetchMembershipGroups,
  fetchMembershipTiers,
  updateMembershipTier,
  createMembershipTier,
  deleteMembershipTier,
} from '@/api/admin';
import type { MembershipTierResponse } from '@/dto/admin';
import RoleDefaultMappingSection from '@/components/memberships/RoleDefaultMappingSection.vue';

const { getErrorMessage } = useApiError();
const { success: toastSuccess } = useToast();

// === State ===
const groups = ref<string[]>([]);
const groupSearch = ref('');
const selectedGroup = ref('');
const allGroupTiers = ref<Record<string, MembershipTierResponse[]>>({});
const groupsLoading = ref(true);

const selectedTier = ref<MembershipTierResponse | null>(null);
const editMode = ref(false);
const editForm = ref({ displayName: '', priceMonthly: null as number | null, priceYearly: null as number | null, sortOrder: 0 });
const editSaving = ref(false);

// Create tier
const createTierMode = ref(false);
const createTierForm = ref({ tierKey: '', displayName: '', priceMonthly: null as number | null, priceYearly: null as number | null, sortOrder: 0 });
const createTierSaving = ref(false);

// Delete tier
const deleteConfirm = ref(false);
const deleting = ref(false);

const error = ref('');

// === Computed ===
const filteredGroups = computed(() => {
  if (!groupSearch.value) return groups.value;
  const q = groupSearch.value.toLowerCase();
  return groups.value.filter((g) => g.toLowerCase().includes(q));
});

const currentTiers = computed(() => allGroupTiers.value[selectedGroup.value] ?? []);

// === Helpers ===
function formatPrice(price: number | null): string {
  if (price == null) return '-';
  return `₩${price.toLocaleString()}`;
}

function selectGroup(group: string) {
  selectedGroup.value = group;
  selectedTier.value = null;
  editMode.value = false;
  createTierMode.value = false;
  deleteConfirm.value = false;
}

function selectTier(tier: MembershipTierResponse) {
  editMode.value = false;
  createTierMode.value = false;
  deleteConfirm.value = false;
  selectedTier.value = tier;
}

function enterEditMode() {
  if (!selectedTier.value) return;
  editForm.value = {
    displayName: selectedTier.value.displayName,
    priceMonthly: selectedTier.value.priceMonthly,
    priceYearly: selectedTier.value.priceYearly,
    sortOrder: selectedTier.value.sortOrder,
  };
  editMode.value = true;
}

async function saveEdit() {
  if (!selectedTier.value) return;
  editSaving.value = true;
  error.value = '';
  try {
    const updated = await updateMembershipTier(selectedTier.value.id, {
      displayName: editForm.value.displayName,
      priceMonthly: editForm.value.priceMonthly,
      priceYearly: editForm.value.priceYearly,
      sortOrder: editForm.value.sortOrder,
    });
    const group = updated.membershipGroup;
    const tiers = allGroupTiers.value[group];
    if (tiers) {
      const idx = tiers.findIndex((t) => t.id === updated.id);
      if (idx >= 0) tiers[idx] = updated;
    }
    selectedTier.value = updated;
    editMode.value = false;
    toastSuccess('Tier updated successfully');
  } catch (e: unknown) {
    error.value = getErrorMessage(e, 'Failed to update tier');
  } finally {
    editSaving.value = false;
  }
}

function enterCreateTierMode() {
  if (!selectedGroup.value) return;
  selectedTier.value = null;
  editMode.value = false;
  deleteConfirm.value = false;
  createTierForm.value = { tierKey: '', displayName: '', priceMonthly: null, priceYearly: null, sortOrder: 0 };
  createTierMode.value = true;
}

async function handleCreateTier() {
  if (!selectedGroup.value) return;
  createTierSaving.value = true;
  error.value = '';
  try {
    const created = await createMembershipTier({
      membershipGroup: selectedGroup.value,
      tierKey: createTierForm.value.tierKey,
      displayName: createTierForm.value.displayName,
      priceMonthly: createTierForm.value.priceMonthly,
      priceYearly: createTierForm.value.priceYearly,
      sortOrder: createTierForm.value.sortOrder,
    });
    if (!allGroupTiers.value[selectedGroup.value]) {
      allGroupTiers.value[selectedGroup.value] = [];
    }
    allGroupTiers.value[selectedGroup.value]!.push(created);
    createTierMode.value = false;
    selectedTier.value = created;
    toastSuccess('Tier created successfully');
  } catch (e: unknown) {
    error.value = getErrorMessage(e, 'Failed to create tier');
  } finally {
    createTierSaving.value = false;
  }
}

async function handleDeleteTier() {
  if (!selectedTier.value) return;
  deleting.value = true;
  error.value = '';
  try {
    const tierId = selectedTier.value.id;
    const group = selectedTier.value.membershipGroup;
    await deleteMembershipTier(tierId);
    const tiers = allGroupTiers.value[group];
    if (tiers) {
      allGroupTiers.value[group] = tiers.filter((t) => t.id !== tierId);
    }
    selectedTier.value = null;
    deleteConfirm.value = false;
    toastSuccess('Tier deleted successfully');
  } catch (e: unknown) {
    error.value = getErrorMessage(e, 'Failed to delete tier');
  } finally {
    deleting.value = false;
  }
}

// === Actions ===
async function loadGroups() {
  groupsLoading.value = true;
  error.value = '';
  try {
    groups.value = await fetchMembershipGroups();
    if (groups.value.length > 0) {
      selectedGroup.value = groups.value[0]!;
      const results = await Promise.all(groups.value.map((g) => fetchMembershipTiers(g)));
      groups.value.forEach((g, i) => {
        allGroupTiers.value[g] = results[i]!;
      });
    }
  } catch (e: unknown) {
    error.value = getErrorMessage(e, 'Failed to load membership groups');
  } finally {
    groupsLoading.value = false;
  }
}

onMounted(loadGroups);
</script>

<template>
  <div>
    <div class="flex items-center justify-between mb-6">
      <h1 class="text-2xl font-bold text-text-heading">Memberships</h1>
      <Button
        v-if="selectedGroup"
        variant="primary"
        size="sm"
        @click="enterCreateTierMode"
      >
        <span class="material-symbols-outlined" style="font-size: 18px;">add</span>
        Add Tier
      </Button>
    </div>

    <!-- Error -->
    <Alert v-if="error" variant="error" dismissible class="mb-4" @dismiss="error = ''">
      {{ error }}
    </Alert>

    <!-- Loading -->
    <div v-if="groupsLoading" class="flex justify-center py-12">
      <Spinner size="lg" />
    </div>

    <!-- 3-Column: Groups(1) | Tiers(1) | Detail(3) -->
    <div v-else class="flex gap-4" style="min-height: calc(100vh - 200px);">
      <!-- Col 1: Membership Groups -->
      <div class="w-52 shrink-0">
        <div class="relative mb-3">
          <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-text-muted" style="font-size: 18px;">search</span>
          <input
            v-model="groupSearch"
            type="text"
            placeholder="Search groups..."
            class="w-full pl-9 pr-3 py-2 bg-bg-card border border-border-default rounded-lg text-sm text-text-body placeholder:text-text-muted focus:outline-none focus:border-border-focus"
          />
        </div>

        <Card variant="elevated" padding="none" class="overflow-hidden">
          <div class="max-h-[calc(100vh-280px)] overflow-y-auto">
            <div v-if="filteredGroups.length === 0" class="px-4 py-8 text-center text-text-meta text-sm">
              No groups found
            </div>
            <div
              v-for="g in filteredGroups"
              :key="g"
              @click="selectGroup(g)"
              :class="[
                'px-4 py-3 cursor-pointer transition-colors border-b border-border-muted last:border-b-0',
                selectedGroup === g
                  ? 'bg-brand-primary/5 border-l-2 border-l-brand-primary'
                  : 'hover:bg-bg-hover border-l-2 border-l-transparent'
              ]"
            >
              <div class="text-sm font-medium text-text-heading">{{ g }}</div>
              <div class="text-[11px] text-text-muted mt-0.5">
                {{ (allGroupTiers[g] ?? []).length }} tiers
              </div>
            </div>
          </div>
        </Card>
      </div>

      <!-- Col 2: Tier List -->
      <div class="w-52 shrink-0">
        <div class="flex items-center mb-3 px-1 py-2 border-b border-border-default min-h-[38px]">
          <Badge v-if="selectedGroup" variant="info" size="sm">{{ selectedGroup }}</Badge>
          <span v-else class="text-xs text-text-muted">Select a group</span>
        </div>

        <Card variant="elevated" padding="none" class="overflow-hidden">
          <div class="max-h-[calc(100vh-280px)] overflow-y-auto">
            <div v-if="!selectedGroup" class="px-4 py-8 text-center text-text-meta text-sm">
              Select a group first
            </div>
            <div v-else-if="currentTiers.length === 0" class="px-4 py-8 text-center text-text-meta text-sm">
              No tiers found
            </div>
            <div
              v-for="tier in currentTiers"
              :key="tier.id"
              @click="selectTier(tier)"
              :class="[
                'px-4 py-3 cursor-pointer transition-colors border-b border-border-muted last:border-b-0',
                selectedTier?.id === tier.id
                  ? 'bg-brand-primary/5 border-l-2 border-l-brand-primary'
                  : 'hover:bg-bg-hover border-l-2 border-l-transparent'
              ]"
            >
              <div class="flex items-center justify-between">
                <span class="text-sm font-medium text-text-heading">{{ tier.displayName }}</span>
                <span class="text-[11px] text-text-muted">#{{ tier.sortOrder }}</span>
              </div>
              <div class="text-xs text-text-meta font-mono mt-0.5">{{ tier.tierKey }}</div>
              <div class="flex items-center gap-3 mt-1 text-[11px] text-text-muted">
                <span v-if="tier.priceMonthly != null">{{ formatPrice(tier.priceMonthly) }}/mo</span>
                <span v-if="tier.priceYearly != null">{{ formatPrice(tier.priceYearly) }}/yr</span>
                <span v-if="tier.priceMonthly == null && tier.priceYearly == null">Free</span>
              </div>
            </div>
          </div>
        </Card>
      </div>

      <!-- Col 3: Tier Detail / Create -->
      <div class="flex-1 min-w-0 flex flex-col">
        <!-- Create Tier Form -->
        <Card v-if="createTierMode" variant="elevated" padding="lg" class="flex-1">
          <h2 class="text-base font-semibold text-text-heading mb-4">Create New Tier</h2>
          <p class="text-sm text-text-meta mb-4">Group: <Badge variant="info" size="sm">{{ selectedGroup }}</Badge></p>

          <div class="space-y-4 mb-4">
            <div class="grid grid-cols-2 gap-4">
              <Input v-model="createTierForm.tierKey" label="Tier Key" placeholder="e.g. PREMIUM" required />
              <Input v-model="createTierForm.displayName" label="Display Name" placeholder="e.g. Premium" required />
            </div>
            <div class="grid grid-cols-2 gap-4">
              <Input v-model="createTierForm.priceMonthly" label="Monthly Price" type="number" placeholder="null = free" />
              <Input v-model="createTierForm.priceYearly" label="Yearly Price" type="number" placeholder="null = free" />
            </div>
            <Input v-model="createTierForm.sortOrder" label="Sort Order" type="number" required />
          </div>

          <div class="flex gap-2">
            <Button variant="primary" size="sm" :loading="createTierSaving" @click="handleCreateTier">
              Create
            </Button>
            <Button variant="ghost" size="sm" @click="createTierMode = false">
              Cancel
            </Button>
          </div>
        </Card>

        <!-- Tier Detail -->
        <template v-else-if="selectedTier">
          <Card variant="elevated" padding="none" class="flex-1">
            <!-- Header -->
            <div class="p-5 border-b border-border-default">
              <div class="flex items-start justify-between">
                <div>
                  <h2 class="text-lg font-semibold text-text-heading">{{ selectedTier.displayName }}</h2>
                  <div class="flex items-center gap-2 mt-1">
                    <Badge variant="default" size="sm">{{ selectedTier.tierKey }}</Badge>
                    <Badge variant="info" size="sm">{{ selectedTier.membershipGroup }}</Badge>
                  </div>
                </div>
                <div v-if="!editMode" class="flex gap-2">
                  <Button variant="outline" size="sm" @click="enterEditMode">
                    <span class="material-symbols-outlined" style="font-size: 16px;">edit</span>
                    Edit
                  </Button>
                  <Button variant="danger" size="sm" @click="deleteConfirm = true">
                    <span class="material-symbols-outlined" style="font-size: 16px;">delete</span>
                    Delete
                  </Button>
                </div>
              </div>

              <!-- Delete Confirmation -->
              <Alert v-if="deleteConfirm" variant="error" class="mt-4">
                <p class="text-sm mb-2">Are you sure you want to delete tier <strong>{{ selectedTier.tierKey }}</strong>?</p>
                <div class="flex gap-2">
                  <Button variant="danger" size="sm" :loading="deleting" @click="handleDeleteTier">
                    Confirm Delete
                  </Button>
                  <Button variant="ghost" size="sm" @click="deleteConfirm = false">
                    Cancel
                  </Button>
                </div>
              </Alert>

              <!-- Edit Mode -->
              <div v-if="editMode" class="mt-4 space-y-3">
                <Input v-model="editForm.displayName" label="Display Name" required />
                <div class="grid grid-cols-2 gap-4">
                  <Input v-model="editForm.priceMonthly" label="Monthly Price" type="number" placeholder="null = free" />
                  <Input v-model="editForm.priceYearly" label="Yearly Price" type="number" placeholder="null = free" />
                </div>
                <Input v-model="editForm.sortOrder" label="Sort Order" type="number" required />
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
            </div>

            <!-- Info -->
            <div v-if="!editMode" class="p-5">
              <h3 class="text-sm font-semibold text-text-heading mb-3">Info</h3>
              <dl class="grid grid-cols-2 gap-x-6 gap-y-2 text-sm">
                <dt class="text-text-meta">Tier Key</dt>
                <dd class="text-text-body font-mono">{{ selectedTier.tierKey }}</dd>
                <dt class="text-text-meta">Membership Group</dt>
                <dd class="text-text-body">{{ selectedTier.membershipGroup }}</dd>
                <dt class="text-text-meta">Monthly Price</dt>
                <dd class="text-text-body">{{ formatPrice(selectedTier.priceMonthly) }}</dd>
                <dt class="text-text-meta">Yearly Price</dt>
                <dd class="text-text-body">{{ formatPrice(selectedTier.priceYearly) }}</dd>
                <dt class="text-text-meta">Sort Order</dt>
                <dd class="text-text-body">{{ selectedTier.sortOrder }}</dd>
              </dl>
            </div>
          </Card>
        </template>

        <!-- Empty State -->
        <div v-else class="flex-1 flex flex-col items-center justify-center text-text-muted bg-bg-card border border-border-default rounded-lg">
          <span class="material-symbols-outlined mb-3" style="font-size: 48px; opacity: 0.3;">card_membership</span>
          <p class="text-sm">Select a tier to view details</p>
        </div>
      </div>
    </div>

    <!-- Role-Default Mapping Section -->
    <div class="mt-6">
      <RoleDefaultMappingSection />
    </div>
  </div>
</template>
