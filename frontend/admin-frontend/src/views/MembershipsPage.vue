<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { Badge, Spinner, Alert, Select, useApiError } from '@portal/design-vue';
import type { SelectOption } from '@portal/design-vue';
import {
  fetchMembershipGroups,
  fetchMembershipTiers,
  fetchUserMemberships,
  changeUserMembership,
  searchUsers,
} from '@/api/admin';
import type { MembershipTierResponse, MembershipResponse, AdminUserSummary } from '@/dto/admin';

const { getErrorMessage } = useApiError();

// === State ===
const groups = ref<string[]>([]);
const selectedGroup = ref('');
const allGroupTiers = ref<Record<string, MembershipTierResponse[]>>({});

const groupsLoading = ref(true);
const tiersLoading = ref(false);
const error = ref('');
const success = ref('');

// Expandable rows
const expandedTier = ref<number | null>(null);

// User search
const searchQuery = ref('');
const searchResults = ref<AdminUserSummary[]>([]);
const searchLoading = ref(false);
const showSearchResults = ref(false);

// Selected user memberships
const selectedUser = ref<AdminUserSummary | null>(null);
const userMemberships = ref<MembershipResponse[]>([]);
const membershipsLoading = ref(false);

// Tier change
const changingGroup = ref<string | null>(null);

// === Computed ===
const currentTiers = computed(() => allGroupTiers.value[selectedGroup.value] ?? []);

function tierOptionsForGroup(group: string, currentTierKey: string): SelectOption[] {
  const tiers = allGroupTiers.value[group] ?? [];
  return tiers
    .filter((t) => t.tierKey !== currentTierKey)
    .map((t) => ({ value: t.tierKey, label: `${t.displayName} (${t.tierKey})` }));
}

function membershipForGroup(group: string): MembershipResponse | undefined {
  return userMemberships.value.find((m) => m.membershipGroup === group);
}

function formatPrice(price: number | null): string {
  if (price == null) return '-';
  return `₩${price.toLocaleString()}`;
}

function formatDate(date: string | null): string {
  if (!date) return '-';
  return new Date(date).toLocaleDateString('ko-KR');
}

function statusVariant(status: string): 'success' | 'danger' | 'warning' {
  switch (status) {
    case 'ACTIVE': return 'success';
    case 'CANCELLED': return 'danger';
    case 'EXPIRED': return 'warning';
    default: return 'warning';
  }
}

function toggleExpand(tierId: number) {
  expandedTier.value = expandedTier.value === tierId ? null : tierId;
}

// === Actions ===
async function loadGroups() {
  groupsLoading.value = true;
  error.value = '';
  try {
    groups.value = await fetchMembershipGroups();
    if (groups.value.length > 0) {
      selectedGroup.value = groups.value[0]!;
      await loadAllTiers(groups.value);
    }
  } catch (e: unknown) {
    error.value = getErrorMessage(e, 'Failed to load membership groups');
  } finally {
    groupsLoading.value = false;
  }
}

async function loadAllTiers(groupList: string[]) {
  tiersLoading.value = true;
  try {
    const results = await Promise.all(groupList.map((g) => fetchMembershipTiers(g)));
    groupList.forEach((g, i) => {
      allGroupTiers.value[g] = results[i]!;
    });
  } catch (e: unknown) {
    error.value = getErrorMessage(e, 'Failed to load tiers');
  } finally {
    tiersLoading.value = false;
  }
}

async function handleSearch() {
  const q = searchQuery.value.trim();
  if (!q) return;

  const uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
  if (uuidPattern.test(q)) {
    await loadUserMemberships(q);
    showSearchResults.value = false;
    return;
  }

  searchLoading.value = true;
  showSearchResults.value = true;
  try {
    const page = await searchUsers(q, 1, 10);
    searchResults.value = page.items;
  } catch (e: unknown) {
    error.value = getErrorMessage(e, 'Failed to search users');
  } finally {
    searchLoading.value = false;
  }
}

async function selectUser(user: AdminUserSummary) {
  selectedUser.value = user;
  showSearchResults.value = false;
  await loadUserMemberships(user.uuid);
}

async function loadUserMemberships(userId: string) {
  membershipsLoading.value = true;
  error.value = '';
  try {
    userMemberships.value = await fetchUserMemberships(userId);
    if (!selectedUser.value || selectedUser.value.uuid !== userId) {
      selectedUser.value = { uuid: userId, email: userId, username: null, nickname: null, profileImageUrl: null, status: 'ACTIVE', createdAt: '', lastLoginAt: null };
    }
  } catch (e: unknown) {
    error.value = getErrorMessage(e, 'Failed to load user memberships');
  } finally {
    membershipsLoading.value = false;
  }
}

async function handleChangeTier(group: string, tierKey: string | null) {
  if (!selectedUser.value || !tierKey) return;
  changingGroup.value = group;
  error.value = '';
  success.value = '';
  try {
    await changeUserMembership(selectedUser.value.uuid, group, tierKey);
    success.value = `Tier changed to ${tierKey} for ${group}`;
    await loadUserMemberships(selectedUser.value.uuid);
  } catch (e: unknown) {
    error.value = getErrorMessage(e, 'Failed to change membership tier');
  } finally {
    changingGroup.value = null;
  }
}

onMounted(loadGroups);
</script>

<template>
  <div>
    <h1 class="text-2xl font-bold text-text-heading mb-6">Memberships</h1>

    <!-- Error / Success -->
    <Alert v-if="error" variant="error" dismissible class="mb-4" @dismiss="error = ''">
      {{ error }}
    </Alert>
    <Alert v-if="success" variant="success" dismissible class="mb-4" @dismiss="success = ''">
      {{ success }}
    </Alert>

    <!-- Loading -->
    <div v-if="groupsLoading" class="flex justify-center py-12">
      <Spinner size="lg" />
    </div>

    <template v-else>
      <!-- Section 1: Tier Configuration -->
      <section class="mb-8">
        <h2 class="text-base font-semibold text-text-heading mb-4">Tier Configuration</h2>

        <!-- Group Tabs -->
        <div class="flex gap-1 mb-4 border-b border-border-default">
          <button
            v-for="g in groups"
            :key="g"
            @click="selectedGroup = g; expandedTier = null"
            :class="[
              'px-4 py-2.5 text-sm font-medium transition-colors border-b-2 -mb-px',
              selectedGroup === g
                ? 'border-brand-primary text-brand-primary'
                : 'border-transparent text-text-meta hover:text-text-body'
            ]"
          >
            {{ g }}
          </button>
        </div>

        <!-- Tier Table -->
        <div v-if="tiersLoading" class="flex justify-center py-8">
          <Spinner size="md" />
        </div>
        <div v-else class="bg-bg-card border border-border-default rounded-lg overflow-hidden">
          <table class="admin-table">
            <thead>
              <tr>
                <th>Tier Key</th>
                <th>Display Name</th>
                <th>Monthly</th>
                <th>Yearly</th>
                <th>Order</th>
                <th class="w-12"></th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="currentTiers.length === 0">
                <td colspan="6" class="text-center text-text-meta py-8">No tiers found</td>
              </tr>
              <template v-for="tier in currentTiers" :key="tier.id">
                <tr class="cursor-pointer" @click="toggleExpand(tier.id)">
                  <td>
                    <span class="font-mono text-xs px-2 py-0.5 bg-bg-elevated rounded">{{ tier.tierKey }}</span>
                  </td>
                  <td class="font-medium">{{ tier.displayName }}</td>
                  <td>{{ formatPrice(tier.priceMonthly) }}</td>
                  <td>{{ formatPrice(tier.priceYearly) }}</td>
                  <td class="text-text-meta">{{ tier.sortOrder }}</td>
                  <td>
                    <span
                      class="material-symbols-outlined text-text-muted transition-transform"
                      :class="{ 'rotate-180': expandedTier === tier.id }"
                      style="font-size: 18px;"
                    >
                      expand_more
                    </span>
                  </td>
                </tr>
                <!-- Expanded Detail -->
                <tr v-if="expandedTier === tier.id" class="admin-table-expandable">
                  <td colspan="6">
                    <div class="grid grid-cols-2 gap-4 text-sm max-w-lg">
                      <div>
                        <span class="text-text-meta text-xs">Membership Group</span>
                        <p class="text-text-body">{{ tier.membershipGroup }}</p>
                      </div>
                      <div>
                        <span class="text-text-meta text-xs">Sort Order</span>
                        <p class="text-text-body">{{ tier.sortOrder }}</p>
                      </div>
                      <div>
                        <span class="text-text-meta text-xs">Monthly Price</span>
                        <p class="text-text-body">{{ formatPrice(tier.priceMonthly) }}</p>
                      </div>
                      <div>
                        <span class="text-text-meta text-xs">Yearly Price</span>
                        <p class="text-text-body">{{ formatPrice(tier.priceYearly) }}</p>
                      </div>
                    </div>
                    <p class="text-xs text-text-muted mt-3">Tier editing will be available in Phase 3</p>
                  </td>
                </tr>
              </template>
            </tbody>
          </table>
        </div>
      </section>

      <!-- Section 2: User Membership -->
      <section>
        <h2 class="text-base font-semibold text-text-heading mb-4">User Membership</h2>

        <!-- Search Bar -->
        <div class="admin-filter-bar">
          <div class="relative flex-1 max-w-lg">
            <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-text-muted" style="font-size: 18px;">search</span>
            <input
              v-model="searchQuery"
              type="text"
              placeholder="Search by email, username or UUID..."
              class="w-full pl-9 pr-3 py-2 bg-bg-elevated border border-border-default rounded text-sm text-text-body placeholder:text-text-muted focus:outline-none focus:border-border-focus"
              @keyup.enter="handleSearch"
            />
          </div>
          <button
            :disabled="searchLoading"
            @click="handleSearch"
            class="px-4 py-2 bg-brand-primary text-white rounded text-sm font-medium hover:bg-brand-primaryHover disabled:opacity-40 transition-colors"
          >
            Search
          </button>
        </div>

        <!-- Search Results Dropdown -->
        <div
          v-if="showSearchResults && searchResults.length > 0"
          class="bg-bg-card border border-border-default rounded-lg overflow-hidden mb-4 max-w-lg"
        >
          <div
            v-for="user in searchResults"
            :key="user.uuid"
            class="px-4 py-3 hover:bg-bg-hover cursor-pointer border-b border-border-muted last:border-b-0 transition-colors"
            @click="selectUser(user)"
          >
            <div class="text-sm text-text-body font-medium">{{ user.email }}</div>
            <div class="text-xs text-text-meta">
              {{ user.username ?? '-' }} · {{ user.uuid.slice(0, 8) }}...
            </div>
          </div>
        </div>

        <div
          v-if="showSearchResults && !searchLoading && searchResults.length === 0"
          class="text-sm text-text-meta mb-4"
        >
          No users found
        </div>

        <!-- User Membership Detail -->
        <div v-if="membershipsLoading" class="flex justify-center py-8">
          <Spinner size="md" />
        </div>

        <template v-else-if="selectedUser">
          <!-- User Info Header -->
          <div class="bg-bg-card border border-border-default rounded-lg p-4 mb-4">
            <div class="flex items-center gap-3">
              <div class="w-10 h-10 rounded-full bg-bg-elevated flex items-center justify-center">
                <span class="material-symbols-outlined text-text-meta" style="font-size: 20px;">person</span>
              </div>
              <div>
                <div class="text-sm font-semibold text-text-heading">{{ selectedUser.email }}</div>
                <div class="text-xs text-text-meta font-mono">{{ selectedUser.uuid }}</div>
              </div>
            </div>
          </div>

          <!-- Membership Cards per Group -->
          <div class="space-y-3">
            <div
              v-for="group in groups"
              :key="group"
              class="bg-bg-card border border-border-default rounded-lg p-4"
            >
              <div class="flex items-start justify-between">
                <div>
                  <h4 class="text-sm font-semibold text-text-heading mb-2">{{ group }}</h4>
                  <template v-if="membershipForGroup(group)">
                    <div class="flex items-center gap-2 mb-2">
                      <Badge variant="info" size="sm">
                        {{ membershipForGroup(group)!.tierDisplayName }}
                      </Badge>
                      <Badge
                        :variant="statusVariant(membershipForGroup(group)!.status)"
                        size="sm"
                      >
                        {{ membershipForGroup(group)!.status }}
                      </Badge>
                    </div>
                    <dl class="grid grid-cols-2 gap-x-4 gap-y-1 text-xs">
                      <dt class="text-text-meta">Tier Key</dt>
                      <dd class="text-text-body font-mono">{{ membershipForGroup(group)!.tierKey }}</dd>
                      <dt class="text-text-meta">Auto-renew</dt>
                      <dd class="text-text-body">{{ membershipForGroup(group)!.autoRenew ? 'Yes' : 'No' }}</dd>
                      <dt class="text-text-meta">Started</dt>
                      <dd class="text-text-body">{{ formatDate(membershipForGroup(group)!.startedAt) }}</dd>
                      <dt class="text-text-meta">Expires</dt>
                      <dd class="text-text-body">{{ formatDate(membershipForGroup(group)!.expiresAt) }}</dd>
                    </dl>
                  </template>
                  <p v-else class="text-sm text-text-meta">No membership</p>
                </div>

                <!-- Change Tier -->
                <div class="flex items-end gap-2 min-w-[200px]">
                  <Select
                    :options="tierOptionsForGroup(group, membershipForGroup(group)?.tierKey ?? '')"
                    placeholder="Change tier..."
                    size="sm"
                    clearable
                    class="flex-1"
                    @update:model-value="(val: string | number) => handleChangeTier(group, String(val))"
                  />
                  <Spinner v-if="changingGroup === group" size="sm" />
                </div>
              </div>
            </div>
          </div>
        </template>
      </section>
    </template>
  </div>
</template>
