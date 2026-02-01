<script setup lang="ts">
import { authService } from "../services/authService";
import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { useAuthStore } from "../store/auth";
import { Card, Spinner, Button } from "@portal/design-system-vue";

const router = useRouter();
const authStore = useAuthStore();
const error = ref<string>('');

onMounted(async () => {
  try {
    const hash = window.location.hash.substring(1);
    const params = new URLSearchParams(hash);
    const accessToken = params.get('access_token');

    if (accessToken) {
      authService.setTokens(accessToken);
      const userInfo = authService.getUserInfo();
      authStore.setAuthenticated(true);
      authStore.setUser(userInfo);
      router.push("/");
    } else {
      throw new Error('No tokens received');
    }
  } catch (err) {
    console.error('OAuth callback error:', err);
    error.value = '로그인 처리 중 오류가 발생했습니다.';
  }
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
        <Button variant="primary" @click="router.push('/')">
          홈으로 돌아가기
        </Button>
      </div>
    </Card>
  </div>
</template>
