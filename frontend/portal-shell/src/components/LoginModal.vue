<script setup lang="ts">
import { ref, computed } from 'vue';
import { Modal, Input, Button } from '@portal/design-system-vue';
import { useAuthStore } from '../store/auth';
import { socialLogin } from '../services/authService';

defineProps<{
  modelValue: boolean;
}>();

const emit = defineEmits<{
  'update:modelValue': [value: boolean];
}>();

const authStore = useAuthStore();

const email = ref('');
const password = ref('');
const isLoading = ref(false);
const error = ref('');

const emailError = ref('');
const passwordError = ref('');

// Check if local environment (Google login available)
const isLocalEnv = computed(() => {
  const apiBase = import.meta.env.VITE_API_BASE_URL || '';
  return apiBase.includes('localhost');
});

function validate(): boolean {
  let isValid = true;

  emailError.value = '';
  passwordError.value = '';
  error.value = '';

  if (!email.value) {
    emailError.value = '이메일을 입력해주세요.';
    isValid = false;
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value)) {
    emailError.value = '올바른 이메일 형식이 아닙니다.';
    isValid = false;
  }

  if (!password.value) {
    passwordError.value = '비밀번호를 입력해주세요.';
    isValid = false;
  } else if (password.value.length < 6) {
    passwordError.value = '비밀번호는 6자 이상이어야 합니다.';
    isValid = false;
  }

  return isValid;
}

async function handleLogin() {
  if (!validate()) return;

  isLoading.value = true;
  error.value = '';

  try {
    // Login with email + password (Direct JWT)
    await authStore.login(email.value, password.value);

    // Success - close modal
    emit('update:modelValue', false);

    // Reset form
    email.value = '';
    password.value = '';

  } catch (err: any) {
    console.error('Login failed:', err);
    error.value = err.message || '로그인에 실패했습니다. 다시 시도해주세요.';
  } finally {
    isLoading.value = false;
  }
}

function handleSocialLogin(provider: 'google' | 'naver' | 'kakao') {
  socialLogin(provider);
}

function handleClose() {
  emit('update:modelValue', false);
  // Reset form
  email.value = '';
  password.value = '';
  error.value = '';
  emailError.value = '';
  passwordError.value = '';
}
</script>

<template>
  <Modal
      :model-value="modelValue"
      @update:model-value="handleClose"
      title="로그인"
      size="sm"
  >
    <form @submit.prevent="handleLogin" class="space-y-5">
      <!-- Welcome Message -->
      <div class="text-center mb-6">
        <div class="w-14 h-14 bg-brand-primary rounded-xl flex items-center justify-center mx-auto mb-4">
          <span class="text-white text-xl">🔐</span>
        </div>
        <p class="text-text-meta">
          Portal Universe에 오신 것을 환영합니다
        </p>
      </div>

      <!-- Email Input -->
      <Input
          v-model="email"
          type="email"
          label="이메일"
          placeholder="your@email.com"
          required
          :error="!!emailError"
          :error-message="emailError"
          :disabled="isLoading"
      />

      <!-- Password Input -->
      <Input
          v-model="password"
          type="password"
          label="비밀번호"
          placeholder="••••••••"
          required
          :error="!!passwordError"
          :error-message="passwordError"
          :disabled="isLoading"
      />

      <!-- Error Message -->
      <div v-if="error" class="p-3 bg-status-errorBg border border-status-error/20 rounded-lg">
        <p class="text-sm text-status-error">{{ error }}</p>
      </div>

      <!-- Forgot Password -->
      <div class="text-right">
        <button
            type="button"
            class="text-sm text-brand-primary hover:text-brand-primaryHover hover:underline"
            :disabled="isLoading"
        >
          비밀번호를 잊으셨나요?
        </button>
      </div>

      <!-- Login Button -->
      <Button
          type="submit"
          variant="primary"
          :disabled="isLoading"
          class="w-full"
      >
        {{ isLoading ? '로그인 중...' : '로그인' }}
      </Button>

      <!-- Divider -->
      <div class="relative my-6">
        <div class="absolute inset-0 flex items-center">
          <div class="w-full border-t border-border-default"></div>
        </div>
        <div class="relative flex justify-center text-sm">
          <span class="px-4 bg-bg-elevated text-text-meta">또는</span>
        </div>
      </div>

      <!-- Social Login Buttons -->
      <div class="space-y-3">
        <!-- Google (Local 환경에서만 표시) -->
        <Button
            v-if="isLocalEnv"
            type="button"
            variant="secondary"
            :disabled="isLoading"
            class="w-full"
            @click="handleSocialLogin('google')"
        >
          <span class="flex items-center justify-center gap-2">
            <svg class="w-5 h-5" viewBox="0 0 24 24">
              <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
              <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
              <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
              <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
            </svg>
            <span>Google로 로그인</span>
          </span>
        </Button>

        <!-- Naver (모든 환경에서 표시) -->
        <Button
            type="button"
            :disabled="isLoading"
            class="w-full bg-[#03C75A] hover:bg-[#02b351] text-white border-none"
            @click="handleSocialLogin('naver')"
        >
          <span class="flex items-center justify-center gap-2">
            <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
              <path d="M16.273 12.845L7.376 0H0v24h7.727V11.155L16.624 24H24V0h-7.727v12.845z"/>
            </svg>
            <span>네이버로 로그인</span>
          </span>
        </Button>

        <!-- Kakao (모든 환경에서 표시) -->
        <Button
            type="button"
            :disabled="isLoading"
            class="w-full bg-[#FEE500] hover:bg-[#fdd835] text-[#191919] border-none"
            @click="handleSocialLogin('kakao')"
        >
          <span class="flex items-center justify-center gap-2">
            <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12 3c5.799 0 10.5 3.664 10.5 8.185 0 4.52-4.701 8.184-10.5 8.184a13.5 13.5 0 0 1-1.727-.11l-4.408 2.883c-.501.265-.678.236-.472-.413l.892-3.678c-2.88-1.46-4.785-3.99-4.785-6.866C1.5 6.665 6.201 3 12 3zm5.907 8.06l1.47-1.424a.472.472 0 0 0-.656-.678l-1.928 1.866V9.282a.472.472 0 0 0-.944 0v2.557a.471.471 0 0 0 0 .222v2.476a.472.472 0 0 0 .944 0v-1.95l.333-.323 1.59 2.162a.472.472 0 0 0 .76-.56l-1.57-2.036zm-6.442-1.823a.472.472 0 0 0-.471.472v3.827a.472.472 0 0 0 .943 0v-3.355h1.177a.472.472 0 0 0 0-.944h-1.65zm-3.02.472v2.412h1.61a.472.472 0 0 0 0-.944h-1.138v-.524h1.138a.472.472 0 0 0 0-.944H8.444v-.471h1.61a.472.472 0 0 0 0-.944H7.973a.472.472 0 0 0-.472.472c0 .165 0 .946-.056.943zm-2.19 3.07a.472.472 0 0 0 .472-.472V9.756a.472.472 0 0 0-.943 0v3.551c0 .26.21.472.471.472z"/>
            </svg>
            <span>카카오로 로그인</span>
          </span>
        </Button>
      </div>

      <!-- Sign Up Link -->
      <div class="text-center text-sm text-text-meta">
        계정이 없으신가요?
        <button
            type="button"
            class="text-brand-primary hover:text-brand-primaryHover font-medium hover:underline"
            :disabled="isLoading"
        >
          회원가입
        </button>
      </div>
    </form>
  </Modal>
</template>
