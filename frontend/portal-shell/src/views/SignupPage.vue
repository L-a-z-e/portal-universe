<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { signup, type SignupRequest } from '@/api/users';
import { Button, Card, Input } from '@portal/design-system';

const router = useRouter();

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
    alert('회원가입이 완료되었습니다.');
    router.push('/'); // 현재 홈에 로그인이 있으므로 홈으로 이동
  } catch (err: any) {
    console.error('Signup failed:', err);
    errors.value.submit = err.response?.data?.message || '회원가입 중 오류가 발생했습니다.';
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

        <div class="flex items-start gap-3 py-2">
          <input
            id="marketing"
            type="checkbox"
            v-model="form.marketingAgree"
            class="mt-1 w-4 h-4 rounded border-border-default text-brand-primary focus:ring-brand-primary cursor-pointer"
          />
          <label for="marketing" class="text-sm text-text-body cursor-pointer select-none">
            마케팅 정보 수신 및 이벤트 알림 동의 (선택)
          </label>
        </div>

        <div v-if="errors.submit" class="p-3 rounded-lg bg-status-errorBg border border-status-error/20 text-status-error text-sm font-medium">
          {{ errors.submit }}
        </div>

        <Button
          type="submit"
          variant="primary"
          size="lg"
          class="w-full mt-4 shadow-lg shadow-brand-primary/20"
          :disabled="loading"
        >
          <span v-if="loading" class="flex items-center justify-center gap-2">
            <svg class="animate-spin h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
            처리 중...
          </span>
          <span v-else>회원가입 완료</span>
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

<style scoped>
/* 추가적인 스타일이 필요한 경우 여기에 작성 */
</style>
