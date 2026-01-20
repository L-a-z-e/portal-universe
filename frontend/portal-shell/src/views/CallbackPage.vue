<script setup lang="ts">
import userManager from "../services/authService.ts";
import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { Spinner, Card } from '@portal/design-system-vue';

const router = useRouter();
const error = ref<string>('');

onMounted(() => {
  userManager.signinRedirectCallback()
      .then(() => {
        router.push("/");
      })
      .catch((err) => {
        console.error('OAuth callback error:', err);
        error.value = '로그인 처리 중 오류가 발생했습니다.';
      });
})
</script>

<template>
  <div class="min-h-[calc(100vh-200px)] flex items-center justify-center p-4 bg-bg-page">
    <Card variant="elevated" padding="lg" class="w-full max-w-md">
      <div v-if="!error" class="text-center py-8">
        <Spinner size="lg" class="mx-auto mb-6" />
        <h2 class="text-2xl font-bold text-text-heading mb-2">로그인 처리 중...</h2>
        <p class="text-text-meta">
          잠시만 기다려 주세요. 곧 메인 페이지로 이동합니다.
        </p>
      </div>

      <div v-else class="text-center py-8">
        <div class="text-6xl mb-6">⚠️</div>
        <h2 class="text-2xl font-bold text-status-error mb-2">로그인 실패</h2>
        <p class="text-text-meta mb-6">{{ error }}</p>
        <button
            @click="router.push('/')"
            class="px-6 py-3 bg-brand-primary text-white font-semibold rounded-lg hover:bg-brand-primaryHover transition-all"
        >
          홈으로 돌아가기
        </button>
      </div>
    </Card>
  </div>
</template>