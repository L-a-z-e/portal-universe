<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { useAuthStore } from '../store/auth';
import { useRouter } from 'vue-router';
import { profileService, type ProfileResponse, type UpdateProfileRequest } from '../services/profileService';
import { useToast, useApiError, Input, Checkbox, Modal, Button, Spinner, Alert, Textarea } from '@portal/design-vue';
import { ROLES, ROLE_PREFIX } from '../constants/roles';

const authStore = useAuthStore();
const router = useRouter();
const toast = useToast();
const { handleError } = useApiError();

// Redirect if not authenticated
if (!authStore.isAuthenticated) {
  router.push('/');
}

// ====================================================================
// State
// ====================================================================

// Server profile data
const serverProfile = ref<ProfileResponse | null>(null);
const loading = ref(false);
const error = ref<string | null>(null);

// Edit mode state
const isEditing = ref(false);
const editForm = ref<UpdateProfileRequest>({
  nickname: '',
  realName: '',
  phoneNumber: '',
  marketingAgree: false,
});
const saveLoading = ref(false);

// Password modal state
const showPasswordModal = ref(false);
const passwordForm = ref({
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
});
const passwordLoading = ref(false);
const passwordError = ref<string | null>(null);

// Delete modal state
const showDeleteModal = ref(false);
const deleteForm = ref({
  password: '',
  reason: '',
});
const deleteLoading = ref(false);
const deleteError = ref<string | null>(null);

// ====================================================================
// Computed
// ====================================================================

// JWT profile from auth store (fallback)
const jwtProfile = computed(() => authStore.user?.profile);
const authority = computed(() => authStore.user?.authority);

// Display profile (prefer server data, fallback to JWT)
const displayProfile = computed(() => {
  if (serverProfile.value) {
    return {
      email: serverProfile.value.email,
      nickname: serverProfile.value.nickname,
      realName: serverProfile.value.realName,
      phoneNumber: serverProfile.value.phoneNumber,
      profileImageUrl: serverProfile.value.profileImageUrl,
      marketingAgree: serverProfile.value.marketingAgree,
      hasSocialAccount: serverProfile.value.hasSocialAccount,
      socialProviders: serverProfile.value.socialProviders,
      createdAt: serverProfile.value.createdAt,
      uuid: serverProfile.value.uuid,
    };
  }
  // Fallback to JWT profile
  return {
    email: jwtProfile.value?.email || '',
    nickname: jwtProfile.value?.nickname || jwtProfile.value?.name || '',
    realName: null,
    phoneNumber: null,
    profileImageUrl: jwtProfile.value?.picture || null,
    marketingAgree: false,
    hasSocialAccount: false,
    socialProviders: [],
    createdAt: null,
    uuid: jwtProfile.value?.sub || '',
  };
});

// Get initials for avatar
const initials = computed(() => {
  const name = displayProfile.value.nickname || authStore.displayName;
  if (!name || name === 'Guest') return 'U';
  return name
    .split(' ')
    .map((n) => n.charAt(0))
    .join('')
    .toUpperCase()
    .substring(0, 2);
});

// Role badges
const roleBadges = computed(() => {
  return authority.value?.roles.map((role) => {
    const displayName = role.replace(ROLE_PREFIX, '');
    const isAdmin = role === ROLES.ADMIN;
    return {
      name: displayName,
      color: isAdmin ? 'bg-status-error' : 'bg-brand-primary',
    };
  }) || [];
});

// Social provider display info
const socialProviderInfo = computed(() => {
  const providers = displayProfile.value.socialProviders;
  if (!providers || providers.length === 0) return [];

  return providers.map((provider) => {
    switch (provider.toUpperCase()) {
      case 'GOOGLE':
        return { name: 'Google', icon: '🔵', color: 'bg-blue-500' };
      case 'NAVER':
        return { name: 'Naver', icon: '🟢', color: 'bg-green-500' };
      case 'KAKAO':
        return { name: 'Kakao', icon: '🟡', color: 'bg-yellow-500' };
      default:
        return { name: provider, icon: '🔗', color: 'bg-gray-500' };
    }
  });
});

// Check if user can change password (not social-only user)
const canChangePassword = computed(() => {
  return !displayProfile.value.hasSocialAccount || serverProfile.value === null;
});

// ====================================================================
// Methods
// ====================================================================

// Fetch profile from server
const fetchProfile = async () => {
  loading.value = true;
  error.value = null;

  try {
    serverProfile.value = await profileService.getProfile();
  } catch (err: unknown) {
    console.error('Failed to fetch profile:', err);
    const { message } = handleError(err, '프로필을 불러오는 데 실패했습니다.');
    error.value = message;
  } finally {
    loading.value = false;
  }
};

// Start editing
const startEditing = () => {
  editForm.value = {
    nickname: displayProfile.value.nickname || '',
    realName: displayProfile.value.realName || '',
    phoneNumber: displayProfile.value.phoneNumber || '',
    marketingAgree: displayProfile.value.marketingAgree || false,
  };
  isEditing.value = true;
};

// Cancel editing
const cancelEditing = () => {
  isEditing.value = false;
  editForm.value = {
    nickname: '',
    realName: '',
    phoneNumber: '',
    marketingAgree: false,
  };
};

// Save profile
const saveProfile = async () => {
  saveLoading.value = true;
  error.value = null;

  try {
    const updated = await profileService.updateProfile(editForm.value);
    serverProfile.value = updated;
    isEditing.value = false;
  } catch (err: unknown) {
    console.error('Failed to save profile:', err);
    const { message } = handleError(err, '프로필 저장에 실패했습니다.');
    error.value = message;
  } finally {
    saveLoading.value = false;
  }
};

// Open password modal
const openPasswordModal = () => {
  passwordForm.value = {
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  };
  passwordError.value = null;
  showPasswordModal.value = true;
};

// Change password
const changePassword = async () => {
  // Validation
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    passwordError.value = '새 비밀번호가 일치하지 않습니다';
    return;
  }

  if (passwordForm.value.newPassword.length < 8) {
    passwordError.value = '비밀번호는 8자 이상이어야 합니다';
    return;
  }

  passwordLoading.value = true;
  passwordError.value = null;

  try {
    await profileService.changePassword(passwordForm.value);
    showPasswordModal.value = false;
    toast.success('비밀번호가 변경되었습니다.');
  } catch (err: unknown) {
    console.error('Failed to change password:', err);
    const { message } = handleError(err, '비밀번호 변경에 실패했습니다.');
    passwordError.value = message;
  } finally {
    passwordLoading.value = false;
  }
};

// Open delete modal
const openDeleteModal = () => {
  deleteForm.value = {
    password: '',
    reason: '',
  };
  deleteError.value = null;
  showDeleteModal.value = true;
};

// Delete account
const deleteAccount = async () => {
  deleteLoading.value = true;
  deleteError.value = null;

  try {
    await profileService.deleteAccount(deleteForm.value);
    showDeleteModal.value = false;

    // Clear auth state and redirect
    authStore.setAuthenticated(false);
    authStore.setUser(null);

    toast.success('회원 탈퇴가 완료되었습니다.');
    router.push('/');
  } catch (err: unknown) {
    console.error('Failed to delete account:', err);
    const { message } = handleError(err, '회원 탈퇴에 실패했습니다.');
    deleteError.value = message;
  } finally {
    deleteLoading.value = false;
  }
};

// Format date
const formatDate = (dateStr?: string | null): string => {
  if (!dateStr) return '-';
  return new Date(dateStr).toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });
};

// ====================================================================
// Lifecycle
// ====================================================================

onMounted(() => {
  if (authStore.isAuthenticated) {
    fetchProfile();
  }
});

// Watch for auth changes
watch(() => authStore.isAuthenticated, (isAuth) => {
  if (isAuth) {
    fetchProfile();
  }
});
</script>

<template>
  <div class="max-w-3xl mx-auto px-4 py-8">
    <!-- Header -->
    <div class="mb-8">
      <h1 class="text-2xl font-bold text-text-heading mb-2">My Profile</h1>
      <p class="text-text-meta">Manage your account information</p>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="flex justify-center py-12">
      <Spinner size="lg" />
    </div>

    <!-- Error Alert -->
    <Alert v-if="error" variant="error" :title="error" dismissible class="mb-6">
      <Button variant="ghost" size="sm" @click="fetchProfile">Retry</Button>
    </Alert>

    <template v-if="!loading && (displayProfile.email || jwtProfile)">
      <!-- Profile Card -->
      <div class="bg-bg-card rounded-xl border border-border-default overflow-hidden mb-6">
        <!-- Profile Header -->
        <div class="bg-gradient-to-r from-brand-primary/20 to-brand-secondary/20 p-8">
          <div class="flex items-center gap-6">
            <!-- Avatar -->
            <div class="relative">
              <div
                v-if="displayProfile.profileImageUrl"
                class="w-24 h-24 rounded-full overflow-hidden border-4 border-white shadow-lg"
              >
                <img
                  :src="displayProfile.profileImageUrl"
                  :alt="displayProfile.nickname"
                  class="w-full h-full object-cover"
                />
              </div>
              <div
                v-else
                class="w-24 h-24 rounded-full bg-brand-primary flex items-center justify-center border-4 border-white shadow-lg"
              >
                <span class="text-3xl font-bold text-white">{{ initials }}</span>
              </div>
            </div>

            <!-- Basic Info -->
            <div>
              <h2 class="text-2xl font-bold text-text-heading">{{ displayProfile.nickname || authStore.displayName }}</h2>
              <p class="text-text-meta">{{ displayProfile.email }}</p>

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
            <div class="flex justify-between items-center mb-4">
              <h3 class="text-sm font-semibold text-text-meta uppercase tracking-wider">
                Basic Information
              </h3>
              <Button
                v-if="!isEditing"
                variant="ghost"
                size="sm"
                @click="startEditing"
              >
                Edit
              </Button>
            </div>

            <!-- View Mode -->
            <div v-if="!isEditing" class="grid gap-4 md:grid-cols-2">
              <div class="space-y-1">
                <label class="text-xs text-text-meta">Email</label>
                <p class="text-text-body font-medium">{{ displayProfile.email }}</p>
              </div>
              <div class="space-y-1">
                <label class="text-xs text-text-meta">Nickname</label>
                <p class="text-text-body font-medium">{{ displayProfile.nickname || '-' }}</p>
              </div>
              <div class="space-y-1">
                <label class="text-xs text-text-meta">Real Name</label>
                <p class="text-text-body font-medium">{{ displayProfile.realName || '-' }}</p>
              </div>
              <div class="space-y-1">
                <label class="text-xs text-text-meta">Phone Number</label>
                <p class="text-text-body font-medium">{{ displayProfile.phoneNumber || '-' }}</p>
              </div>
            </div>

            <!-- Edit Mode -->
            <div v-else class="space-y-4">
              <div class="grid gap-4 md:grid-cols-2">
                <Input
                  type="email"
                  label="Email (cannot be changed)"
                  :model-value="displayProfile.email"
                  disabled
                />
                <Input
                  v-model="editForm.nickname"
                  label="Nickname"
                  placeholder="Enter nickname"
                />
                <Input
                  v-model="editForm.realName"
                  label="Real Name"
                  placeholder="Enter real name"
                />
                <Input
                  v-model="editForm.phoneNumber"
                  type="tel"
                  label="Phone Number"
                  placeholder="Enter phone number"
                />
              </div>

              <Checkbox
                v-model="editForm.marketingAgree"
                label="I agree to receive marketing information"
              />

              <div class="flex gap-3 pt-2">
                <Button
                  variant="primary"
                  :loading="saveLoading"
                  :disabled="saveLoading"
                  @click="saveProfile"
                >
                  Save Changes
                </Button>
                <Button
                  variant="secondary"
                  :disabled="saveLoading"
                  @click="cancelEditing"
                >
                  Cancel
                </Button>
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
                <p class="text-text-body font-mono text-sm">{{ displayProfile.uuid }}</p>
              </div>
              <div class="space-y-1">
                <label class="text-xs text-text-meta">Created At</label>
                <p class="text-text-body">{{ formatDate(displayProfile.createdAt) }}</p>
              </div>
              <div class="space-y-1">
                <label class="text-xs text-text-meta">Marketing Agreement</label>
                <p class="flex items-center gap-2">
                  <span
                    :class="[
                      'w-2 h-2 rounded-full',
                      displayProfile.marketingAgree ? 'bg-status-success' : 'bg-status-warning'
                    ]"
                  />
                  <span class="text-text-body">{{ displayProfile.marketingAgree ? 'Agreed' : 'Not Agreed' }}</span>
                </p>
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
                v-for="provider in socialProviderInfo"
                :key="provider.name"
                class="flex items-center justify-between p-4 bg-bg-elevated rounded-lg"
              >
                <div class="flex items-center gap-3">
                  <span class="text-2xl">{{ provider.icon }}</span>
                  <div>
                    <p class="font-medium text-text-body">{{ provider.name }}</p>
                    <p class="text-xs text-text-meta">Connected via OAuth2</p>
                  </div>
                </div>
                <span class="px-2 py-1 bg-status-success/10 text-status-success text-xs rounded-md font-medium">
                  Connected
                </span>
              </div>
              <div v-if="socialProviderInfo.length === 0" class="text-center py-8 text-text-meta">
                <p>No social accounts connected</p>
              </div>
            </div>
          </section>
        </div>
      </div>

      <!-- Account Actions -->
      <div class="bg-bg-card rounded-xl border border-border-default p-6 mb-6">
        <h3 class="text-lg font-semibold text-text-heading mb-4">Account Actions</h3>
        <div class="flex gap-3 flex-wrap items-center">
          <Button
            variant="primary"
            :disabled="!canChangePassword"
            @click="openPasswordModal"
          >
            Change Password
          </Button>
          <p v-if="!canChangePassword" class="text-sm text-text-meta">
            (Social login users cannot change password)
          </p>
        </div>
      </div>

      <!-- Danger Zone -->
      <div class="bg-bg-card rounded-xl border border-status-error/30 p-6">
        <h3 class="text-lg font-semibold text-status-error mb-4">Danger Zone</h3>
        <p class="text-text-meta mb-4">
          Account deletion is a permanent action and cannot be undone.
        </p>
        <Button variant="danger" @click="openDeleteModal">
          Delete Account
        </Button>
      </div>
    </template>

    <!-- Not Authenticated -->
    <template v-else-if="!loading">
      <div class="bg-bg-card rounded-xl border border-border-default p-12 text-center">
        <div class="text-5xl mb-4">🔒</div>
        <h2 class="text-xl font-semibold text-text-heading mb-2">Not Authenticated</h2>
        <p class="text-text-meta mb-6">Please log in to view your profile.</p>
        <Button variant="primary" size="lg" @click="router.push('/')">
          Go to Home
        </Button>
      </div>
    </template>

    <!-- Password Change Modal -->
    <Modal v-model="showPasswordModal" title="Change Password" size="sm">
      <Alert v-if="passwordError" variant="error" :title="passwordError" class="mb-4" />

      <div class="space-y-4">
        <Input
          v-model="passwordForm.currentPassword"
          type="password"
          label="Current Password"
          placeholder="Enter current password"
        />
        <Input
          v-model="passwordForm.newPassword"
          type="password"
          label="New Password"
          placeholder="Enter new password (min 8 characters)"
        />
        <Input
          v-model="passwordForm.confirmPassword"
          type="password"
          label="Confirm New Password"
          placeholder="Confirm new password"
        />
      </div>

      <div class="flex gap-3 mt-6">
        <Button
          variant="primary"
          :loading="passwordLoading"
          :disabled="passwordLoading"
          full-width
          @click="changePassword"
        >
          Change Password
        </Button>
        <Button
          variant="secondary"
          :disabled="passwordLoading"
          @click="showPasswordModal = false"
        >
          Cancel
        </Button>
      </div>
    </Modal>

    <!-- Delete Account Modal -->
    <Modal v-model="showDeleteModal" title="Delete Account" size="sm">
      <p class="text-text-body mb-4">
        Are you sure you want to delete your account? This action cannot be undone.
      </p>

      <Alert v-if="deleteError" variant="error" :title="deleteError" class="mb-4" />

      <div class="space-y-4">
        <Input
          v-if="!displayProfile.hasSocialAccount"
          v-model="deleteForm.password"
          type="password"
          label="Password"
          placeholder="Enter your password to confirm"
        />
        <Textarea
          v-model="deleteForm.reason"
          label="Reason (Optional)"
          :rows="3"
          placeholder="Why are you leaving?"
        />
      </div>

      <div class="flex gap-3 mt-6">
        <Button
          variant="danger"
          :loading="deleteLoading"
          :disabled="deleteLoading || (!displayProfile.hasSocialAccount && !deleteForm.password)"
          full-width
          @click="deleteAccount"
        >
          Delete Account
        </Button>
        <Button
          variant="secondary"
          :disabled="deleteLoading"
          @click="showDeleteModal = false"
        >
          Cancel
        </Button>
      </div>
    </Modal>
  </div>
</template>
