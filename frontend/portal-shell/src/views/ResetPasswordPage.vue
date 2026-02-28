<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { validateResetToken, resetPassword } from '@/api/passwordReset';
import { getPasswordPolicy, type PasswordPolicyResponse } from '@/api/users';
import { Button, Card, Input, Alert, useToast, useApiError } from '@portal/design-vue';

const router = useRouter();
const route = useRoute();
const toast = useToast();
const { handleError } = useApiError();

const token = computed(() => (route.query.token as string) || '');
const tokenValid = ref<boolean | null>(null);
const loading = ref(false);
const validating = ref(true);
const resetComplete = ref(false);
const passwordPolicy = ref<PasswordPolicyResponse | null>(null);

const newPassword = ref('');
const confirmPassword = ref('');
const errors = ref({ newPassword: '', confirmPassword: '', submit: '' });

onMounted(async () => {
  if (!token.value) {
    tokenValid.value = false;
    validating.value = false;
    return;
  }

  try {
    const [valid] = await Promise.all([
      validateResetToken(token.value),
      getPasswordPolicy().catch(() => null),
    ]);
    tokenValid.value = valid;
    if (!valid) return;
  } catch {
    tokenValid.value = false;
  } finally {
    validating.value = false;
  }

  try {
    passwordPolicy.value = await getPasswordPolicy();
  } catch {
    passwordPolicy.value = { minLength: 8, maxLength: 128, requirements: ['8자 이상 입력해주세요'] };
  }
});

function validate(): boolean {
  errors.value = { newPassword: '', confirmPassword: '', submit: '' };
  let valid = true;

  const minLen = passwordPolicy.value?.minLength ?? 8;
  if (!newPassword.value || newPassword.value.length < minLen) {
    errors.value.newPassword = `비밀번호는 ${minLen}자 이상이어야 합니다.`;
    valid = false;
  }

  if (!confirmPassword.value) {
    errors.value.confirmPassword = '비밀번호 확인을 입력해주세요.';
    valid = false;
  } else if (newPassword.value !== confirmPassword.value) {
    errors.value.confirmPassword = '비밀번호가 일치하지 않습니다.';
    valid = false;
  }

  return valid;
}

async function handleSubmit() {
  if (!validate()) return;

  loading.value = true;
  errors.value.submit = '';
  try {
    await resetPassword(token.value, newPassword.value, confirmPassword.value);
    resetComplete.value = true;
    toast.success('비밀번호가 변경되었습니다.');
  } catch (err: any) {
    const message = err?.response?.data?.message || '비밀번호 재설정에 실패했습니다.';
    errors.value.submit = message;
    handleError(err, message);
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="min-h-screen flex items-center justify-center px-4 bg-bg-default">
    <Card class="w-full max-w-md p-8">
      <!-- 검증 중 -->
      <template v-if="validating">
        <div class="text-center space-y-4">
          <p class="text-text-meta">토큰을 검증하고 있습니다...</p>
        </div>
      </template>

      <!-- 토큰 무효 -->
      <template v-else-if="!tokenValid">
        <div class="text-center space-y-4">
          <div class="w-16 h-16 bg-status-errorBg rounded-full flex items-center justify-center mx-auto">
            <span class="text-3xl">⚠️</span>
          </div>
          <h2 class="text-xl font-bold text-text-primary">유효하지 않은 링크</h2>
          <p class="text-text-meta text-sm">
            비밀번호 재설정 링크가 만료되었거나 유효하지 않습니다.
            다시 비밀번호 찾기를 진행해주세요.
          </p>
          <Button
            variant="primary"
            class="w-full"
            @click="router.push('/forgot-password')"
          >
            비밀번호 찾기
          </Button>
        </div>
      </template>

      <!-- 재설정 완료 -->
      <template v-else-if="resetComplete">
        <div class="text-center space-y-4">
          <div class="w-16 h-16 bg-status-successBg rounded-full flex items-center justify-center mx-auto">
            <span class="text-3xl">✅</span>
          </div>
          <h2 class="text-xl font-bold text-text-primary">비밀번호 변경 완료</h2>
          <p class="text-text-meta text-sm">
            비밀번호가 성공적으로 변경되었습니다. 새 비밀번호로 로그인해주세요.
          </p>
          <Button
            variant="primary"
            class="w-full"
            @click="router.push('/')"
          >
            로그인하러 가기
          </Button>
        </div>
      </template>

      <!-- 비밀번호 재설정 폼 -->
      <template v-else>
        <div class="text-center mb-6">
          <h1 class="text-2xl font-bold text-text-primary mb-2">새 비밀번호 설정</h1>
          <p class="text-text-meta text-sm">
            새로운 비밀번호를 입력해주세요.
          </p>
        </div>

        <form @submit.prevent="handleSubmit" class="space-y-5">
          <Input
            v-model="newPassword"
            type="password"
            label="새 비밀번호"
            placeholder="••••••••"
            required
            :error="!!errors.newPassword"
            :error-message="errors.newPassword"
            :disabled="loading"
          />

          <Input
            v-model="confirmPassword"
            type="password"
            label="비밀번호 확인"
            placeholder="••••••••"
            required
            :error="!!errors.confirmPassword"
            :error-message="errors.confirmPassword"
            :disabled="loading"
          />

          <!-- Password Policy Hint -->
          <div v-if="passwordPolicy" class="text-xs text-text-meta space-y-1">
            <p v-for="req in passwordPolicy.requirements" :key="req">• {{ req }}</p>
          </div>

          <Alert v-if="errors.submit" variant="error" :title="errors.submit" />

          <Button
            type="submit"
            variant="primary"
            :disabled="loading"
            class="w-full"
          >
            {{ loading ? '변경 중...' : '비밀번호 변경' }}
          </Button>
        </form>
      </template>
    </Card>
  </div>
</template>
