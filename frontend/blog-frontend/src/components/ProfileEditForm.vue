<script setup lang="ts">
import { ref, computed } from 'vue';
import { Button, Input, Textarea, Alert } from '@portal/design-vue';
import type { UserProfileResponse, UserProfileUpdateRequest } from '@/dto/user';
import { updateProfile, setUsername, checkUsername } from '@/api/users';

interface Props {
  user: UserProfileResponse;
}
const props = defineProps<Props>();

const emit = defineEmits<{
  success: [user: UserProfileResponse];
  cancel: [];
}>();

// Form data
const formData = ref<UserProfileUpdateRequest & { username?: string }>({
  name: props.user.nickname,
  bio: props.user.bio || '',
  website: props.user.website || '',
  username: props.user.username || '',
});

// Username 관련 상태
const usernameCheckLoading = ref(false);
const usernameAvailable = ref<boolean | null>(null);
const usernameMessage = ref('');
const usernameDebounceTimer = ref<ReturnType<typeof setTimeout> | null>(null);

// 로딩 및 에러
const loading = ref(false);
const error = ref('');

// Username 수정 가능 여부 (최초 설정 시만 가능)
const canEditUsername = computed(() => !props.user.username);

// Username 유효성 검증
const isValidUsername = (username: string): boolean => {
  const regex = /^[a-zA-Z0-9_-]{3,20}$/;
  return regex.test(username);
};

// Username 중복 확인 (디바운스)
const handleUsernameChange = (value: string | number) => {
  const strValue = String(value);
  formData.value.username = strValue;

  if (!canEditUsername.value) return;

  // 기존 타이머 취소
  if (usernameDebounceTimer.value) {
    clearTimeout(usernameDebounceTimer.value);
  }

  usernameAvailable.value = null;
  usernameMessage.value = '';

  if (!strValue) return;

  // 유효성 검증
  if (!isValidUsername(strValue)) {
    usernameAvailable.value = false;
    usernameMessage.value = 'Username은 3-20자의 영문, 숫자, _, - 만 사용 가능합니다.';
    return;
  }

  // 디바운스 (500ms)
  usernameDebounceTimer.value = setTimeout(async () => {
    usernameCheckLoading.value = true;
    try {
      const result = await checkUsername(strValue);
      usernameAvailable.value = result.available;
      usernameMessage.value = result.message;
    } catch (err: any) {
      usernameAvailable.value = false;
      usernameMessage.value = '중복 확인 중 오류가 발생했습니다.';
    } finally {
      usernameCheckLoading.value = false;
    }
  }, 500);
};

// 폼 제출
const handleSubmit = async () => {
  loading.value = true;
  error.value = '';

  try {
    // 1. Username 설정 (최초 설정인 경우)
    if (canEditUsername.value && formData.value.username) {
      if (!usernameAvailable.value) {
        error.value = 'Username을 확인해주세요.';
        return;
      }
      await setUsername(formData.value.username);
    }

    // 2. 프로필 정보 업데이트
    const updateData: UserProfileUpdateRequest = {
      name: formData.value.name,
      bio: formData.value.bio,
      website: formData.value.website,
    };

    const updatedUser = await updateProfile(updateData);
    emit('success', updatedUser);
  } catch (err: any) {
    error.value = err.response?.data?.message || '프로필 업데이트에 실패했습니다.';
  } finally {
    loading.value = false;
  }
};

// 취소
const handleCancel = () => {
  emit('cancel');
};
</script>

<template>
  <div class="profile-edit-form">
    <h2 class="form-title">프로필 수정</h2>

    <Alert v-if="error" variant="error" class="mb-4">
      {{ error }}
    </Alert>

    <form @submit.prevent="handleSubmit" class="form-container">
      <!-- 이름 -->
      <div class="form-field">
        <label class="field-label" for="name">이름 *</label>
        <Input
          id="name"
          v-model="formData.name"
          placeholder="이름을 입력하세요"
          required
        />
      </div>

      <!-- Username -->
      <div class="form-field">
        <label class="field-label" for="username">
          Username
          <span v-if="canEditUsername" class="field-hint">(최초 1회만 설정 가능)</span>
        </label>
        <Input
          id="username"
          :model-value="formData.username"
          @update:model-value="handleUsernameChange"
          placeholder="username (3-20자)"
          :disabled="!canEditUsername"
        />
        <p
          v-if="usernameCheckLoading"
          class="field-message loading"
        >
          확인 중...
        </p>
        <p
          v-else-if="usernameMessage"
          :class="['field-message', usernameAvailable ? 'success' : 'error']"
        >
          {{ usernameMessage }}
        </p>
      </div>

      <!-- Bio -->
      <div class="form-field">
        <label class="field-label" for="bio">소개</label>
        <Textarea
          id="bio"
          v-model="formData.bio"
          placeholder="자기소개를 입력하세요"
          :rows="4"
          :maxlength="200"
        />
        <p class="field-hint">
          {{ formData.bio?.length || 0 }} / 200
        </p>
      </div>

      <!-- Website -->
      <div class="form-field">
        <label class="field-label" for="website">웹사이트</label>
        <Input
          id="website"
          v-model="formData.website"
          type="url"
          placeholder="https://example.com"
        />
      </div>

      <!-- 버튼 -->
      <div class="form-actions">
        <Button
          variant="secondary"
          type="button"
          @click="handleCancel"
          :disabled="loading"
        >
          취소
        </Button>
        <Button
          variant="primary"
          type="submit"
          :loading="loading"
        >
          저장
        </Button>
      </div>
    </form>
  </div>
</template>

<style scoped>
.profile-edit-form {
  width: 100%;
  max-width: 600px;
  margin: 0 auto;
}

.form-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--semantic-text-heading);
  margin-bottom: 1.5rem;
}

.form-container {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.field-label {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--semantic-text-heading);
}

.field-hint {
  font-size: 0.75rem;
  font-weight: 400;
  color: var(--semantic-text-meta);
  margin-left: 0.5rem;
}

.field-message {
  font-size: 0.75rem;
  margin-top: 0.25rem;
}

.field-message.loading {
  color: var(--semantic-text-meta);
}

.field-message.success {
  color: var(--semantic-success);
}

.field-message.error {
  color: var(--semantic-error);
}

.form-actions {
  display: flex;
  gap: 0.75rem;
  justify-content: flex-end;
  margin-top: 1rem;
}
</style>
