<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { Card, Button, Badge, Spinner, Alert, SearchBar, Select, useApiError } from '@portal/design-system-vue';
import type { SelectOption } from '@portal/design-system-vue';
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

  // UUID 직접 입력 감지 (UUID v4 패턴)
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
    <h1 class="text-2xl font-bold text-text-heading mb-6">Membership Management</h1>

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
      <section class="mb-6">
        <h2 class="text-lg font-semibold text-text-heading mb-3">Tier Configuration</h2>

        <!-- Group Tabs -->
        <div class="flex gap-2 mb-4">
          <Button
            v-for="g in groups"
            :key="g"
            :variant="selectedGroup === g ? 'primary' : 'outline'"
            size="sm"
            @click="selectedGroup = g"
          >
            {{ g }}
          </Button>
        </div>

        <!-- Tier Table -->
        <div v-if="tiersLoading" class="flex justify-center py-8">
          <Spinner size="md" />
        </div>
        <Card v-else variant="outlined" padding="none">
          <table class="w-full text-sm">
            <thead class="bg-bg-elevated border-b border-border-default">
              <tr>
                <th class="text-left p-3 text-text-heading">Tier Key</th>
                <th class="text-left p-3 text-text-heading">Display Name</th>
                <th class="text-left p-3 text-text-heading">Monthly</th>
                <th class="text-left p-3 text-text-heading">Yearly</th>
                <th class="text-left p-3 text-text-heading">Order</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="currentTiers.length === 0">
                <td colspan="5" class="p-4 text-center text-text-meta">No tiers found</td>
              </tr>
              <tr
                v-for="tier in currentTiers"
                :key="tier.id"
                class="border-b border-border-default"
              >
                <td class="p-3 font-mono text-text-body">
                  <Badge variant="outline" size="sm">{{ tier.tierKey }}</Badge>
                </td>
                <td class="p-3 text-text-body">{{ tier.displayName }}</td>
                <td class="p-3 text-text-body">{{ formatPrice(tier.priceMonthly) }}</td>
                <td class="p-3 text-text-body">{{ formatPrice(tier.priceYearly) }}</td>
                <td class="p-3 text-text-meta">{{ tier.sortOrder }}</td>
              </tr>
            </tbody>
          </table>
        </Card>
      </section>

      <!-- Section 2: User Membership -->
      <section>
        <h2 class="text-lg font-semibold text-text-heading mb-3">User Membership</h2>

        <!-- Search Bar -->
        <div class="flex gap-2 mb-4 relative">
          <SearchBar
            v-model="searchQuery"
            placeholder="Search by email, username or UUID..."
            class="flex-1 max-w-lg"
            @keyup.enter="handleSearch"
          />
          <Button variant="primary" :loading="searchLoading" @click="handleSearch">Search</Button>
        </div>

        <!-- Search Results Dropdown -->
        <Card
          v-if="showSearchResults && searchResults.length > 0"
          variant="outlined"
          padding="none"
          class="mb-4 max-w-lg"
        >
          <ul>
            <li
              v-for="user in searchResults"
              :key="user.uuid"
              class="px-4 py-3 hover:bg-bg-elevated cursor-pointer border-b border-border-default last:border-b-0 transition-colors"
              @click="selectUser(user)"
            >
              <div class="text-sm text-text-body font-medium">{{ user.email }}</div>
              <div class="text-xs text-text-meta">
                {{ user.username ?? '-' }} · {{ user.uuid.slice(0, 8) }}...
              </div>
            </li>
          </ul>
        </Card>

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
          <Card variant="outlined" padding="lg" class="mb-4">
            <div class="flex items-center gap-3 mb-1">
              <h3 class="text-sm font-semibold text-text-heading">
                {{ selectedUser.email }}
              </h3>
              <Badge v-if="selectedUser.username" variant="outline" size="sm">
                @{{ selectedUser.username }}
              </Badge>
            </div>
            <p class="text-xs text-text-meta font-mono">{{ selectedUser.uuid }}</p>
          </Card>

          <!-- Membership Cards per Group -->
          <div class="space-y-3">
            <Card
              v-for="group in groups"
              :key="group"
              variant="outlined"
              padding="lg"
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
                  <p v-else class="text-sm text-text-meta italic">No membership</p>
                </div>

                <!-- Change Tier Select -->
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
            </Card>
          </div>
        </template>
      </section>
    </template>
  </div>
</template>
