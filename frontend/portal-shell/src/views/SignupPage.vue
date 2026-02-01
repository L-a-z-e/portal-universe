<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { signup, type SignupRequest } from '@/api/users';
import { Button, Card, Input, Checkbox, Alert, useToast, useApiError } from '@portal/design-system-vue';

const router = useRouter();
const toast = useToast();
const { handleError } = useApiError();

const form = ref<SignupRequest>({
  email: '',
  password: '',
  nickname: '',
  realName: '',
  marketingAgree: false,
});

const errors = ref({
  email: '',
  password: '',
  nickname: '',
  submit: ''
});

const loading = ref(false);

const validateForm = () => {
  let isValid = true;
  errors.value = { email: '', password: '', nickname: '', submit: '' };

  if (!form.value.email.includes('@')) {
    errors.value.email = '유효한 이메일 주소를 입력해주세요.';
    isValid = false;
  }
  if (!form.value.password || form.value.password.length < 8) {
    errors.value.password = '비밀번호는 8자 이상이어야 합니다.';
    isValid = false;
  }
  if (!form.value.nickname) {
    errors.value.nickname = '닉네임을 입력해주세요.';
    isValid = false;
  }

  return isValid;
};

const handleSignup = async () => {
  if (!validateForm()) return;

  loading.value = true;
  try {
    await signup(form.value);
    toast.success('회원가입이 완료되었습니다.');
    router.push('/');
  } catch (err: unknown) {
    const { message } = handleError(err, '회원가입 중 오류가 발생했습니다.');
    errors.value.submit = message;
  } finally {
    loading.value = false;
  }
};
</script>

<template>
  <div class="min-h-[calc(100vh-200px)] flex items-center justify-center p-4 bg-bg-page">
    <Card variant="elevated" padding="lg" class="w-full max-w-md">
      <div class="text-center mb-8">
        <h1 class="text-3xl font-bold text-text-heading tracking-tight">회원가입</h1>
        <p class="text-text-meta mt-2">Portal Universe에 오신 것을 환영합니다</p>
      </div>

      <form @submit.prevent="handleSignup" class="space-y-5">
        <Input
          label="이메일"
          type="email"
          v-model="form.email"
          placeholder="example@portal.com"
          required
          :error="!!errors.email"
          :error-message="errors.email"
        />

        <Input
          label="비밀번호"
          type="password"
          v-model="form.password"
          placeholder="8자 이상 입력해주세요"
          required
          :error="!!errors.password"
          :error-message="errors.password"
        />

        <Input
          label="닉네임"
          v-model="form.nickname"
          placeholder="사용하실 닉네임을 입력해주세요"
          required
          :error="!!errors.nickname"
          :error-message="errors.nickname"
        />

        <Input
          label="실명 (선택)"
          v-model="form.realName"
          placeholder="실명을 입력해주세요"
        />

        <Checkbox
          v-model="form.marketingAgree"
          label="마케팅 정보 수신 및 이벤트 알림 동의 (선택)"
        />

        <Alert v-if="errors.submit" variant="error" :title="errors.submit" />

        <Button
          type="submit"
          variant="primary"
          size="lg"
          full-width
          :loading="loading"
          :disabled="loading"
          class="mt-4 shadow-lg shadow-brand-primary/20"
        >
          회원가입 완료
        </Button>

        <div class="mt-8 pt-6 border-t border-border-default text-center">
          <p class="text-sm text-text-meta">
            이미 계정이 있으신가요? 
            <router-link to="/" class="text-brand-primary font-bold hover:text-brand-primaryHover ml-1">
              로그인하기
            </router-link>
          </p>
        </div>
      </form>
    </Card>
  </div>
</template>
