<script setup lang="ts">
import { computed } from 'vue';
import { useAuthStore } from '../store/auth';
import { useRouter } from 'vue-router';

const authStore = useAuthStore();
const router = useRouter();

// Redirect if not authenticated
if (!authStore.isAuthenticated) {
  router.push('/');
}

// Profile data from auth store
const profile = computed(() => authStore.user?.profile);
const authority = computed(() => authStore.user?.authority);

// Get initials for avatar
const initials = computed(() => {
  const name = authStore.displayName;
  if (!name || name === 'Guest') return 'U';
  return name
    .split(' ')
    .map((n) => n.charAt(0))
    .join('')
    .toUpperCase()
    .substring(0, 2);
});

// Format date
const formatDate = (timestamp?: number): string => {
  if (!timestamp) return '-';
  return new Date(timestamp * 1000).toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
};

// Get provider from email domain (simple detection)
const socialProvider = computed(() => {
  const email = profile.value?.email;
  if (!email) return null;

  if (email.includes('@gmail.com')) return { name: 'Google', icon: '🔵', color: 'bg-blue-500' };
  if (email.includes('@naver.com')) return { name: 'Naver', icon: '🟢', color: 'bg-green-500' };
  if (email.includes('@kakao.com') || email.includes('@kakaomail.com')) return { name: 'Kakao', icon: '🟡', color: 'bg-yellow-500' };
  return null;
});

// Token info
const tokenExpiresAt = computed(() => authStore.user?._expiresAt);
const tokenIssuedAt = computed(() => authStore.user?._issuedAt);

// Role badges
const roleBadges = computed(() => {
  return authority.value?.roles.map((role) => {
    const displayName = role.replace('ROLE_', '');
    const isAdmin = role === 'ROLE_ADMIN';
    return {
      name: displayName,
      color: isAdmin ? 'bg-status-error' : 'bg-brand-primary',
    };
  }) || [];
});
</script>

<template>
  <div class="max-w-3xl mx-auto px-4 py-8">
    <!-- Header -->
    <div class="mb-8">
      <h1 class="text-2xl font-bold text-text-heading mb-2">My Profile</h1>
      <p class="text-text-meta">View your account information</p>
    </div>

    <template v-if="profile">
      <!-- Profile Card -->
      <div class="bg-bg-card rounded-xl border border-border-default overflow-hidden mb-6">
        <!-- Profile Header -->
        <div class="bg-gradient-to-r from-brand-primary/20 to-brand-secondary/20 p-8">
          <div class="flex items-center gap-6">
            <!-- Avatar -->
            <div class="relative">
              <div
                v-if="profile.picture"
                class="w-24 h-24 rounded-full overflow-hidden border-4 border-white shadow-lg"
              >
                <img
                  :src="profile.picture"
                  :alt="authStore.displayName"
                  class="w-full h-full object-cover"
                />
              </div>
              <div
                v-else
                class="w-24 h-24 rounded-full bg-brand-primary flex items-center justify-center border-4 border-white shadow-lg"
              >
                <span class="text-3xl font-bold text-white">{{ initials }}</span>
              </div>

              <!-- Verified Badge -->
              <div
                v-if="profile.emailVerified"
                class="absolute -bottom-1 -right-1 w-8 h-8 bg-status-success rounded-full flex items-center justify-center border-2 border-white"
                title="Verified Account"
              >
                <svg class="w-4 h-4 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7" />
                </svg>
              </div>
            </div>

            <!-- Basic Info -->
            <div>
              <h2 class="text-2xl font-bold text-text-heading">{{ authStore.displayName }}</h2>
              <p class="text-text-meta">{{ profile.email }}</p>

              <!-- Role Badges -->
              <div class="flex gap-2 mt-3">
                <span
                  v-for="badge in roleBadges"
                  :key="badge.name"
                  :class="['px-2 py-1 rounded-md text-xs font-medium text-white', badge.color]"
                >
                  {{ badge.name }}
                </span>
              </div>
            </div>
          </div>
        </div>

        <!-- Profile Details -->
        <div class="p-6 space-y-6">
          <!-- Basic Information Section -->
          <section>
            <h3 class="text-sm font-semibold text-text-meta uppercase tracking-wider mb-4">
              Basic Information
            </h3>
            <div class="grid gap-4 md:grid-cols-2">
              <div class="space-y-1">
                <label class="text-xs text-text-meta">Email</label>
                <p class="text-text-body font-medium">{{ profile.email }}</p>
              </div>
              <div class="space-y-1">
                <label class="text-xs text-text-meta">Nickname</label>
                <p class="text-text-body font-medium">{{ profile.nickname || '-' }}</p>
              </div>
              <div class="space-y-1">
                <label class="text-xs text-text-meta">Username</label>
                <p class="text-text-body font-medium">{{ profile.username || '-' }}</p>
              </div>
              <div class="space-y-1">
                <label class="text-xs text-text-meta">Full Name</label>
                <p class="text-text-body font-medium">{{ profile.name || '-' }}</p>
              </div>
            </div>
          </section>

          <!-- Divider -->
          <hr class="border-border-default" />

          <!-- Account Information Section -->
          <section>
            <h3 class="text-sm font-semibold text-text-meta uppercase tracking-wider mb-4">
              Account Information
            </h3>
            <div class="grid gap-4 md:grid-cols-2">
              <div class="space-y-1">
                <label class="text-xs text-text-meta">User ID</label>
                <p class="text-text-body font-mono text-sm">{{ profile.sub }}</p>
              </div>
              <div class="space-y-1">
                <label class="text-xs text-text-meta">Email Verified</label>
                <p class="flex items-center gap-2">
                  <span
                    :class="[
                      'w-2 h-2 rounded-full',
                      profile.emailVerified ? 'bg-status-success' : 'bg-status-warning'
                    ]"
                  />
                  <span class="text-text-body">{{ profile.emailVerified ? 'Verified' : 'Not Verified' }}</span>
                </p>
              </div>
              <div class="space-y-1">
                <label class="text-xs text-text-meta">Language</label>
                <p class="text-text-body">{{ profile.locale === 'ko' ? '한국어' : 'English' }}</p>
              </div>
              <div class="space-y-1">
                <label class="text-xs text-text-meta">Timezone</label>
                <p class="text-text-body">{{ profile.timezone || 'Asia/Seoul' }}</p>
              </div>
            </div>
          </section>

          <!-- Divider -->
          <hr class="border-border-default" />

          <!-- Connected Accounts Section -->
          <section>
            <h3 class="text-sm font-semibold text-text-meta uppercase tracking-wider mb-4">
              Connected Accounts
            </h3>
            <div class="space-y-3">
              <div
                v-if="socialProvider"
                class="flex items-center justify-between p-4 bg-bg-elevated rounded-lg"
              >
                <div class="flex items-center gap-3">
                  <span class="text-2xl">{{ socialProvider.icon }}</span>
                  <div>
                    <p class="font-medium text-text-body">{{ socialProvider.name }}</p>
                    <p class="text-xs text-text-meta">Connected via OAuth2</p>
                  </div>
                </div>
                <span class="px-2 py-1 bg-status-success/10 text-status-success text-xs rounded-md font-medium">
                  Connected
                </span>
              </div>
              <div v-else class="text-center py-8 text-text-meta">
                <p>No social accounts connected</p>
              </div>
            </div>
          </section>

          <!-- Divider -->
          <hr class="border-border-default" />

          <!-- Session Information Section -->
          <section>
            <h3 class="text-sm font-semibold text-text-meta uppercase tracking-wider mb-4">
              Session Information
            </h3>
            <div class="grid gap-4 md:grid-cols-2">
              <div class="space-y-1">
                <label class="text-xs text-text-meta">Session Started</label>
                <p class="text-text-body text-sm">{{ formatDate(tokenIssuedAt) }}</p>
              </div>
              <div class="space-y-1">
                <label class="text-xs text-text-meta">Session Expires</label>
                <p class="text-text-body text-sm">{{ formatDate(tokenExpiresAt) }}</p>
              </div>
            </div>
          </section>
        </div>
      </div>

      <!-- Actions (Phase 2 Placeholder) -->
      <div class="bg-bg-card rounded-xl border border-border-default p-6">
        <h3 class="text-lg font-semibold text-text-heading mb-4">Account Actions</h3>
        <p class="text-text-meta mb-4">
          Profile editing, password change, and account deletion features will be available soon.
        </p>
        <div class="flex gap-3">
          <button
            disabled
            class="px-4 py-2 rounded-lg bg-brand-primary/50 text-white cursor-not-allowed"
          >
            Edit Profile (Coming Soon)
          </button>
          <button
            disabled
            class="px-4 py-2 rounded-lg border border-border-default text-text-meta cursor-not-allowed"
          >
            Change Password
          </button>
        </div>
      </div>

      <!-- Danger Zone -->
      <div class="mt-6 bg-bg-card rounded-xl border border-status-error/30 p-6">
        <h3 class="text-lg font-semibold text-status-error mb-4">Danger Zone</h3>
        <p class="text-text-meta mb-4">
          Account deletion is a permanent action and cannot be undone.
        </p>
        <button
          disabled
          class="px-4 py-2 rounded-lg border border-status-error/50 text-status-error/50 cursor-not-allowed"
        >
          Delete Account (Coming Soon)
        </button>
      </div>
    </template>

    <!-- Not Authenticated -->
    <template v-else>
      <div class="bg-bg-card rounded-xl border border-border-default p-12 text-center">
        <div class="text-5xl mb-4">🔒</div>
        <h2 class="text-xl font-semibold text-text-heading mb-2">Not Authenticated</h2>
        <p class="text-text-meta mb-6">Please log in to view your profile.</p>
        <router-link
          to="/"
          class="inline-block px-6 py-2 rounded-lg bg-brand-primary text-white hover:bg-brand-primary/90 transition-colors"
        >
          Go to Home
        </router-link>
      </div>
    </template>
  </div>
</template>
