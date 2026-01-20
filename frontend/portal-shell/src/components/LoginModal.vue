<script setup lang="ts">
import { ref } from 'vue';
import { Modal, Input, Button } from '@portal/design-system-vue';
import { login } from '../services/authService';

defineProps<{
  modelValue: boolean;
}>();

const emit = defineEmits<{
  'update:modelValue': [value: boolean];
}>();

const email = ref('');
const password = ref('');
const isLoading = ref(false);
const error = ref('');

const emailError = ref('');
const passwordError = ref('');

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
    // 실제 로그인 (OIDC)
    await login();

    // 성공하면 Modal 닫기
    emit('update:modelValue', false);

    // 폼 초기화
    email.value = '';
    password.value = '';

  } catch (err: any) {
    console.error('Login failed:', err);
    error.value = err.message || '로그인에 실패했습니다. 다시 시도해주세요.';
  } finally {
    isLoading.value = false;
  }
}

function handleClose() {
  emit('update:modelValue', false);
  // 폼 초기화
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
        <div class="w-14 h-14 bg-[#5e6ad2] rounded-xl flex items-center justify-center mx-auto mb-4">
          <span class="text-white text-xl">🔐</span>
        </div>
        <p class="text-[#6b6b6b] light:text-gray-500">
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
      <div v-if="error" class="p-3 bg-red-500/10 border border-red-500/20 rounded-lg light:bg-red-50 light:border-red-200">
        <p class="text-sm text-red-400 light:text-red-600">{{ error }}</p>
      </div>

      <!-- Forgot Password -->
      <div class="text-right">
        <button
            type="button"
            class="text-sm text-[#5e6ad2] hover:text-[#818cf8] hover:underline"
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
          <div class="w-full border-t border-[#2a2a2a] light:border-gray-200"></div>
        </div>
        <div class="relative flex justify-center text-sm">
          <span class="px-4 bg-[#18191b] text-[#6b6b6b] light:bg-white light:text-gray-500">또는</span>
        </div>
      </div>

      <!-- OAuth Buttons -->
      <div class="space-y-3">
        <Button
            type="button"
            variant="secondary"
            :disabled="isLoading"
            class="w-full"
            @click="login"
        >
          <span class="flex items-center justify-center gap-2">
            <span>🌐</span>
            <span>OIDC로 로그인</span>
          </span>
        </Button>
      </div>

      <!-- Sign Up Link -->
      <div class="text-center text-sm text-[#6b6b6b] light:text-gray-600">
        계정이 없으신가요?
        <button
            type="button"
            class="text-[#5e6ad2] hover:text-[#818cf8] font-medium hover:underline"
            :disabled="isLoading"
        >
          회원가입
        </button>
      </div>
    </form>
  </Modal>
</template>