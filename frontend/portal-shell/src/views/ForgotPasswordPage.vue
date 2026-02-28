<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { requestPasswordReset } from '@/api/passwordReset';
import { Button, Card, Input, Alert, useApiError } from '@portal/design-vue';

const router = useRouter();
const { handleError } = useApiError();

const email = ref('');
const emailError = ref('');
const loading = ref(false);
const submitted = ref(false);

function validate(): boolean {
  emailError.value = '';
  if (!email.value) {
    emailError.value = '이메일을 입력해주세요.';
    return false;
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value)) {
    emailError.value = '올바른 이메일 형식이 아닙니다.';
    return false;
  }
  return true;
}

async function handleSubmit() {
  if (!validate()) return;

  loading.value = true;
  try {
    await requestPasswordReset(email.value);
    submitted.value = true;
  } catch (err: any) {
    handleError(err, '비밀번호 재설정 요청에 실패했습니다.');
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="min-h-screen flex items-center justify-center px-4 bg-bg-default">
    <Card class="w-full max-w-md p-8">
      <!-- 제출 전 -->
      <template v-if="!submitted">
        <div class="text-center mb-6">
          <h1 class="text-2xl font-bold text-text-primary mb-2">비밀번호 찾기</h1>
          <p class="text-text-meta text-sm">
            가입한 이메일 주소를 입력하시면 비밀번호 재설정 링크를 보내드립니다.
          </p>
        </div>

        <form @submit.prevent="handleSubmit" class="space-y-5">
          <Input
            v-model="email"
            type="email"
            label="이메일"
            placeholder="your@email.com"
            required
            :error="!!emailError"
            :error-message="emailError"
            :disabled="loading"
          />

          <Button
            type="submit"
            variant="primary"
            :disabled="loading"
            class="w-full"
          >
            {{ loading ? '전송 중...' : '재설정 링크 전송' }}
          </Button>
        </form>

        <div class="text-center mt-4">
          <Button
            type="button"
            variant="ghost"
            size="sm"
            @click="router.push('/')"
          >
            로그인으로 돌아가기
          </Button>
        </div>
      </template>

      <!-- 제출 후 -->
      <template v-else>
        <div class="text-center space-y-4">
          <div class="w-16 h-16 bg-status-successBg rounded-full flex items-center justify-center mx-auto">
            <span class="text-3xl">✉️</span>
          </div>
          <h2 class="text-xl font-bold text-text-primary">이메일을 확인해주세요</h2>
          <p class="text-text-meta text-sm">
            입력하신 이메일로 비밀번호 재설정 링크를 전송했습니다.
            메일이 도착하지 않으면 스팸 폴더를 확인해주세요.
          </p>
          <Alert variant="info" title="링크는 30분 동안 유효합니다." />
          <Button
            variant="primary"
            class="w-full"
            @click="router.push('/')"
          >
            홈으로 돌아가기
          </Button>
        </div>
      </template>
    </Card>
  </div>
</template>
