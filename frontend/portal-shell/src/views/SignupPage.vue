<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { signup, getPasswordPolicy, type SignupRequest, type PasswordPolicyResponse } from '@/api/users';
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
const showPasswordPolicy = ref(false);
const passwordPolicy = ref<PasswordPolicyResponse | null>(null);

onMounted(async () => {
  try {
    passwordPolicy.value = await getPasswordPolicy();
  } catch {
    // 정책 로드 실패 시 기본값 사용
    passwordPolicy.value = {
      minLength: 8,
      maxLength: 128,
      requirements: ['8자 이상 입력해주세요']
    };
  }
});

const validateForm = () => {
  let isValid = true;
  errors.value = { email: '', password: '', nickname: '', submit: '' };

  if (!form.value.email.includes('@')) {
    errors.value.email = '유효한 이메일 주소를 입력해주세요.';
    isValid = false;
  }

  const minLen = passwordPolicy.value?.minLength ?? 8;
  if (!form.value.password || form.value.password.length < minLen) {
    errors.value.password = `비밀번호는 ${minLen}자 이상이어야 합니다.`;
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

        <div>
          <div class="flex items-center gap-1.5 mb-1.5">
            <label class="block text-sm font-medium text-text-body">
              비밀번호
              <span class="text-status-error ml-0.5">*</span>
            </label>
            <button
              type="button"
              @click="showPasswordPolicy = !showPasswordPolicy"
              class="inline-flex items-center justify-center w-4 h-4 rounded-full
                     text-[10px] font-bold leading-none
                     border border-border-default text-text-muted
                     hover:bg-bg-elevated hover:text-text-body
                     transition-colors cursor-pointer"
              title="비밀번호 조건 보기"
            >
              ?
            </button>
          </div>

          <!-- 비밀번호 정책 안내 -->
          <div
            v-if="showPasswordPolicy && passwordPolicy"
            class="mb-2 p-3 rounded-md bg-bg-elevated border border-border-default text-sm"
          >
            <p class="font-medium text-text-body mb-1.5">비밀번호 조건</p>
            <ul class="space-y-0.5 text-text-meta">
              <li
                v-for="(req, idx) in passwordPolicy.requirements"
                :key="idx"
                class="flex items-start gap-1.5"
              >
                <span class="text-text-muted mt-0.5 shrink-0">&#8226;</span>
                <span>{{ req }}</span>
              </li>
            </ul>
          </div>

          <input
            type="password"
            :value="form.password"
            @input="(e) => form.password = (e.target as HTMLInputElement).value"
            :placeholder="`${passwordPolicy?.minLength ?? 8}자 이상 입력해주세요`"
            :class="[
              'w-full rounded-md h-9 px-3 text-sm',
              'bg-bg-card text-text-body placeholder:text-text-muted',
              'border border-border-default',
              'transition-all duration-150 ease-out',
              'focus:outline-none focus:ring-2 focus:ring-brand-primary/30 focus:border-brand-primary',
              'hover:border-border-hover',
              errors.password
                ? 'border-status-error focus:border-status-error focus:ring-status-error/30'
                : ''
            ]"
          />
          <p
            v-if="errors.password"
            class="mt-1.5 text-sm text-status-error"
          >
            {{ errors.password }}
          </p>
        </div>

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
