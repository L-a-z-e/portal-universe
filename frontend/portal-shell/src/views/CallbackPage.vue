<script setup lang="ts">
import { authService } from "../services/authService";
import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { useAuthStore } from "../store/auth";

const router = useRouter();
const authStore = useAuthStore();
const error = ref<string>('');

onMounted(async () => {
  try {
    // URL Fragment에서 토큰 추출 (OAuth2 콜백)
    const hash = window.location.hash.substring(1);
    const params = new URLSearchParams(hash);

    const accessToken = params.get('access_token');
    const refreshToken = params.get('refresh_token');

    if (accessToken && refreshToken) {
      // 토큰 저장
      authService.setTokens(accessToken, refreshToken);

      // store 업데이트
      const userInfo = authService.getUserInfo();
      authStore.setAuthenticated(true);
      authStore.setUser(userInfo);

      router.push("/");
    } else {
      // 토큰이 없으면 에러
      throw new Error('No tokens received');
    }
  } catch (err) {
    console.error('OAuth callback error:', err);
    error.value = '로그인 처리 중 오류가 발생했습니다.';
  }
})
</script>

<template>
  <div class="min-h-[calc(100vh-200px)] flex items-center justify-center p-4 bg-gray-50 dark:bg-gray-900">
    <div class="w-full max-w-md bg-white dark:bg-gray-800 rounded-xl shadow-lg p-8">
      <div v-if="!error" class="text-center py-8">
        <div class="animate-spin w-8 h-8 border-4 border-blue-500 border-t-transparent rounded-full mx-auto mb-6"></div>
        <h2 class="text-2xl font-bold text-gray-900 dark:text-white mb-2">로그인 처리 중...</h2>
        <p class="text-gray-500 dark:text-gray-400">
          잠시만 기다려 주세요. 곧 메인 페이지로 이동합니다.
        </p>
      </div>

      <div v-else class="text-center py-8">
        <div class="text-6xl mb-6">⚠️</div>
        <h2 class="text-2xl font-bold text-red-500 mb-2">로그인 실패</h2>
        <p class="text-gray-500 dark:text-gray-400 mb-6">{{ error }}</p>
        <button
            @click="router.push('/')"
            class="px-6 py-3 bg-blue-600 text-white font-semibold rounded-lg hover:bg-blue-700 transition-all"
        >
          홈으로 돌아가기
        </button>
      </div>
    </div>
  </div>
</template>